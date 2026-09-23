let params = new URLSearchParams(window.location.search);

const verificationCode = document.getElementById("verificationCode");
verificationCode.value = params.get("verificationCode");
const userEmail = params.get("email")

async function verify_account(){

    Notiflix.Loading.pulse("Wait...",{
        clickToClose:false,
        svgColor:"#0284c7"
    });

    const verifyObj = {
        email:userEmail,
        verificationCode:verificationCode.value
    }
    try{
        const response = await fetch("api/users/verify_account",{
            method: "POST",
            headers: {
                "Content-Type":"application/json"
            },
            body: JSON.stringify(verifyObj)
        });

        if (response.ok){
            const data = await response.json();
            console.log(data)
            if (data.status) {
                Notiflix.Report.success(
                    'Hercey',
                    data.message,
                    'Okay',//button title
                    () => {
                        window.location = "sign_in.html"
                    },
                );
            } else {
                Notiflix.Notify.failure(data.message);
            }
        } else {
            Notiflix.Notify.failure("verification Process failed!");
        }
    }catch (e) {
        Notiflix.Notify.failure(e);
    } finally {
        Notiflix.Loading.remove(1000);
    }
}