async function signOut() {
    Notiflix.Confirm.show(
        'Hercey',
        'Are you sure you want to log out?',
        'Yes, Log out',
        'Cancel',
        // FIX: Added 'async' keyword here
        async () => {

            try {
                // Now you can use await here
                const response = await fetch("api/users/logout", {
                    method: "GET",
                    credentials: "include"
                });

                if (response.status === 202) { // 202 Accepted
                    window.location = "sign_in.html";
                } else {
                    Notiflix.Notify.failure("Logout failed! Please try again.", {
                        position: "center-top"
                    });
                }
            } catch (e) {
                Notiflix.Notify.failure(e.message, {
                    position: "center-top"
                });
            }
        },
        () => {
            // Cancel callback
        },
        {
            titleColor: '#e63946',
            okButtonBackground: '#e63946',
        }
    );
}
document.getElementById("addToCart").addEventListener("click", async ()=>{
    await addToCart(params.get("id"),document.getElementById("qtyInput").value);
});