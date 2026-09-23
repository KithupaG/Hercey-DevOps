import com.hercey.entity.Status;
import com.hercey.mail.VerificationMail;
import com.hercey.provider.MailServiceProvider;
import com.hercey.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;


public class Test {
    public static void main(String[] args) {

//        MailServiceProvider.getInstance().start();
//        VerificationMail verificationMail = new VerificationMail("nimuthuparanawithana2004629@gmail.com", "123456");
//        MailServiceProvider.getInstance().sendMail(verificationMail);

//        String s = AppUtil.generateCode();
//        System.out.println(s);
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
//            User user = s.createQuery("FROM User u WHERE u.id= :id", User.class)
//                    .setParameter("id", 1)
//                    .getSingleResult();

            Status.Type[] values = Status.Type.values();
            Transaction transaction = s.beginTransaction();
            for (Status.Type t : values) {
                Status status = new Status();
                status.setValue(t.name());
                s.persist(status);
            }
            transaction.commit();
        }
    }
}
