async function sign_in(){
    let email = document.getElementById("email");
    let password = document.getElementById("password");

    const loginDetails = {
        email: email.value,
        password : password.value
    };

    const response = await fetch("api/users/login", {
        method : "POST",
        headers : {"Content-Type": "application/json"},
        body : JSON.stringify(loginDetails)
    })

    if (response.ok) {
        const data = await response.json();
        if (data.status) {
            Notiflix.Loading.remove(1000);
            Notiflix.Report.success(
                'Hercey',
                data.message,
                'Okay',
                () => { window.location = "index.html"; }
            );
        } else {
            Notiflix.Notify.failure(data.message, { position: "center-top" });
        }
        console.log("Login successful:", data);
    } else {
        console.error("Login failed:", data);
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