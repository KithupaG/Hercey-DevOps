async function sign_up() {

    let firstName = document.getElementById("fn");
    let lastName = document.getElementById("ln");
    let mobile = document.getElementById("mobile");
    let email = document.getElementById("email");
    let password = document.getElementById("password");
    let confirmPassword = document.getElementById("password2");

    const user = {
        firstName: firstName.value,
        lastName: lastName.value,
        mobile: mobile.value,
        email: email.value,
        password: password.value,
        confirmPassword: confirmPassword.value
    }

    const notification = new Notification();

    try {
        const response = await fetch("api/users", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(user)
        });

        Notiflix.Loading.pulse("Wait...", {
            clickToClose: false,
            svgColor: "#0284c7"
        });

        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                Notiflix.Loading.remove(1000);
                Notiflix.Report.success(
                    'hercey',
                    data.message,
                    'Okay'
                );


            }else {
                Notiflix.Loading.remove();
                Notiflix.Notify.failure(data.message,{
                    position : "center-top"
                });
            }
            console.log(data)
        } else {
            Notiflix.Notify.failure('Something went wrong. Please check your credentials',{
                position : "center-top"
            });
        }

    } catch (e) {
        notification.error({
            title: "Error",
            message: "Oops ! some thing went wrong \n exception : " + e.message
        })
        console.log(e)
    }
}

function togglePassword(inputId, icon) {
    const input = document.getElementById(inputId);
    const isPassword = input.getAttribute('type') === 'password';

    // 1. Toggle the input type (password <-> text)
    input.setAttribute('type', isPassword ? 'text' : 'password');

    // 2. Toggle the icon classes
    // Removes 'bi-eye-fill' and adds 'bi-eye' (or vice versa)
    icon.classList.toggle('bi-eye-slash');
    icon.classList.toggle('bi-eye');
}