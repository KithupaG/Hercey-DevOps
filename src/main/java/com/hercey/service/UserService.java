package com.hercey.service;

import com.google.gson.JsonObject;
import com.hercey.dto.UserDTO;
import java.util.List;
import com.hercey.entity.Cart;
import com.hercey.entity.Seller;
import com.hercey.entity.Status;
import com.hercey.entity.Stock;
import com.hercey.entity.User;
import com.hercey.mail.VerificationMail;
import com.hercey.provider.MailServiceProvider;
import com.hercey.util.AppUtil;
import com.hercey.util.HibernateUtil;
import com.hercey.validation.Validator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Context;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class UserService {

    public String addNewUser(UserDTO userDTO) {

        boolean status = false;
        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status", false);
        String message = "";

        if (userDTO.getFirstName().isBlank()) {
            message = "First Name can not be empty";
        } else if (userDTO.getFirstName() == null) {
            message = "First Name is required";
        } else if (userDTO.getLastName().isBlank()) {
            message = "Last Name can not be empty";
        } else if (userDTO.getLastName() == null) {
            message = "Last Name is required";
        } else if (userDTO.getEmail().isBlank()) {
            message = "Email is can not be empty";
        } else if (userDTO.getEmail() == null) {
            message = "Email is required";
        } else if (!userDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
            message = "Please provide valid email.";
        } else if (userDTO.getMobile().isBlank()) {
            message = "Mobile number can not be empty";
        } else if (!userDTO.getMobile().matches(Validator.MOBILE_VALIDATION)) {
            message = "Check mobile number again";
        } else if (userDTO.getPassword().isBlank()) {
            message = "Password can not be empty";
        } else if (!userDTO.getPassword().equals(userDTO.getConfirmPassword())) {
            message = "Passwords do not match";
        } else if (!userDTO.getPassword().matches(Validator.PASSWORD_VALIDATION)) {
            message = "Please provide valid password. \n" +
                    "The password must contains at least one capital letter, one simple letter, " +
                    "one digit, one special character and password must be greater that 8 characters";
        } else {
            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
            User singleUser = hibernateSession.createNamedQuery("User.getByEmail", User.class).
                    setParameter("email", userDTO.getEmail())
                    .getSingleResultOrNull();
            if (singleUser != null) {
                message = "Email is already registered";
            } else {
                User u = new User();
                u.setFirstName(userDTO.getFirstName());
                u.setLastName(userDTO.getLastName());
                u.setMobile(userDTO.getMobile());
                u.setEmail(userDTO.getEmail());
                u.setPassword(userDTO.getPassword());

                String verificationCode = AppUtil.generateCode();
                u.setVerificationCode(verificationCode);

                Status pendingStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("value", String.valueOf(Status.Type.PENDING)).getSingleResult();

                u.setStatus(pendingStatus);

                Transaction transaction = hibernateSession.beginTransaction();

                try {
                    hibernateSession.persist(u);
                    transaction.commit();
                    status = true;

                    VerificationMail verificationMail = new VerificationMail(u.getEmail(), verificationCode);
                    MailServiceProvider.getInstance().sendMail(verificationMail);

                    message = "Account created Successfully. Verification code has been sent to your email. " +
                            "Please verify it for activate your account!";
                } catch (HibernateException e) {
                    transaction.rollback();
                    message = "Account Creation failed.Please try again!";
                }
            }
            hibernateSession.close();
        }
        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String userLogin(UserDTO userDTO,  @Context HttpServletRequest request) {
        boolean status = false;
        JsonObject responseObject = new JsonObject();
        String message = "";

        if (userDTO.getEmail().isBlank()) {
            message = "Email is required";
        } else if (userDTO.getEmail() == null) {
            message = "Email is required";
        } else if (!userDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
            message = "Please provide valid email.";
        } else if (userDTO.getPassword().isBlank()) {
            message = "Password is required";
        } else if (userDTO.getPassword() == null) {
            message = "Password is required";
        } else {
            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
            User singleUser = hibernateSession.createNamedQuery("User.getByEmail", User.class)
                    .setParameter("email", userDTO.getEmail())
                    .getSingleResultOrNull();
            if (singleUser == null) {
                message = "A user with this email does not exist please register";
            }else  {
                if(!singleUser.getPassword().equals(userDTO.getPassword())) {
                    message = "Please check your login credentials.";
                }else  {
                    Status verifiedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                            .setParameter("value", String.valueOf(Status.Type.VERIFIED))
                            .getSingleResultOrNull();
                    if (!singleUser.getStatus().equals(verifiedStatus)) {
                        message = "your account is not verified. Please verify first!";
                    } else {
                        HttpSession httpSession = request.getSession();
                        httpSession.setAttribute("user", singleUser);

                        Seller seller = hibernateSession.createQuery("FROM Seller s WHERE s.user = :user", Seller.class)
                                .setParameter("user", singleUser)
                                .uniqueResult();
                        boolean isSeller = seller != null && seller.getStatus().getValue().equals(Status.Type.APPROVED.toString());
                        httpSession.setAttribute("isSeller", isSeller);

                        @SuppressWarnings("unchecked")
                        List<Cart> sessionCart = (List<Cart>) httpSession.getAttribute("sessionCart");
                        if (sessionCart != null && !sessionCart.isEmpty()) {
                            Transaction tx = hibernateSession.beginTransaction();
                            for (Cart sc : sessionCart) {
                                Stock scStock = hibernateSession.get(Stock.class, sc.getStock().getId());
                                Cart dbItem = hibernateSession.createQuery(
                                                "FROM Cart c WHERE c.user=:user AND c.stock=:stock", Cart.class)
                                        .setParameter("user", singleUser)
                                        .setParameter("stock", scStock)
                                        .uniqueResult();
                                if (dbItem == null) {
                                    Cart n = new Cart();
                                    n.setUser(singleUser);
                                    n.setStock(scStock);
                                    n.setQty(sc.getQty());
                                    hibernateSession.persist(n);
                                }
                            }
                            tx.commit();
                            httpSession.setAttribute("sessionCart", null);
                        }

                        status = true;
                        message = "Login Successful";
                    }

                }
            }
            hibernateSession.close();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }

    public String verifyUserAccount(UserDTO userDTO) {
        JsonObject responseObject = new JsonObject();
        boolean status = false;
        String message = "";

        if (userDTO.getEmail() == null) {
            message = "Email is required";
        } else if (userDTO.getEmail().isBlank()) {
            message = "Email cannot be empty";
        } else if (!userDTO.getEmail().matches(Validator.EMAIL_VALIDATION)) {
            message = "Please Provide a valid email address";
        } else if (userDTO.getVerificationCode() == null) {
            message = "Verification is required";
        } else if (userDTO.getVerificationCode().isBlank()) {
            message = "Verification code can not be empty";
        } else if (!userDTO.getVerificationCode().matches(Validator.VERIFICATION_CODE_VALIDATION)) {
            message = "Please Provide valid verification code!. Verification code must have 6 digits.";
        } else {
            Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
            User user = hibernateSession.createQuery("FROM User u WHERE u.verificationCode = :verificationCode", User.class)
                    .setParameter("verificationCode", userDTO.getVerificationCode())
                    .getSingleResultOrNull();
            if (user == null) {
                message = "Account Not Found. Please Register First!";
            } else {
                Status verifiedStatus = hibernateSession.createNamedQuery("Status.findByValue", Status.class)
                        .setParameter("value", String.valueOf(Status.Type.VERIFIED))
                        .getSingleResult();
                if (user.getStatus().equals(verifiedStatus)) {
                    message = "Account already Verified";
                } else {
                    user.setStatus(verifiedStatus);
                    user.setVerificationCode("");
                    Transaction transaction = hibernateSession.beginTransaction();
                    try {
                        hibernateSession.merge(user);
                        transaction.commit();
                        status = true;
                        message = "Account Verification Complete.";
                    } catch (HibernateException e) {
                        transaction.rollback();
                        message = "Something went wrong. Verification Process Failed!";
                    }
                }
            }
            hibernateSession.close();
        }

        responseObject.addProperty("status", status);
        responseObject.addProperty("message", message);
        return AppUtil.GSON.toJson(responseObject);
    }
}
