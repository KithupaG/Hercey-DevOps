window.addEventListener("load", async () => {
    await getCities();
    await loadUserData();
    await loadAddress();
    await loadCheckoutSummary();
})

const isCurrentAddressWrap = document.getElementById("isCurrentAddressWrap");
const isCurrentAddressCheckbox = document.getElementById("isCurrentAddress");
const addressSection = document.getElementById('addressSection');
isCurrentAddressCheckbox.addEventListener("change", () => {
    if (isCurrentAddressCheckbox.checked) {
        isCurrentAddressWrap.classList.add("border-primary");
        addressSection.classList.remove('d-none');
    } else {
        isCurrentAddressWrap.classList.remove("border-primary");
        addressSection.classList.add('d-none');

        document.getElementById("lineOne").value = "";
        document.getElementById("lineTwo").value = "";
        document.getElementById("postalCode").value = "";
        document.getElementById("citySelect").value = 0;

    }
});
isCurrentAddressWrap.addEventListener("click", (e) => {
    if (e.target !== isCurrentAddressCheckbox) {
        isCurrentAddressCheckbox.click();
    }
});

async function getCities() {
    try {
        const response = await fetch("api/data/cities")
        if (response.ok) {
            const data = await response.json();
            const citySelect = document.getElementById("citySelect");
            data.cities.forEach((city) => {
                const option = document.createElement("option");
                option.value = city.id;
                option.innerHTML = city.name;
                citySelect.appendChild(option);
            });
        } else {
            Notiflix.Notify.failure("City Loading Failed!", {
                position: "center-top"
            });
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: "center-top"
        });
    }
}

async function loadAddress() {
    try {
        const response = await fetch("api/profiles/addresses");
        if (response.ok) {
            const data = await response.json();
            renderAddresses(data.addresses);
            console.log(data.addresses);
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message);
        console.error(e);
    }
}

function renderAddresses(addresses) {
    const list = document.getElementById("addressList");
    const inputField = document.createElement('address-radio');
    list.innerHTML = "";
    addresses.forEach(addr => {
        const card = document.createElement("address-body");
        card.className = "card shadow-sm p-3 mb-3 " + (addr.isPrimary ? "border-primary-subtle" : "");
        inputField.setAttribute('checked', (addr.isPrimary ? 'checked' : 'un-checked'));
        card.style.backgroundColor = (addr.isPrimary ? "#f8fbff" : "");

        if (addr.isPrimary) {
            document.getElementById("lineOne").value = addr.lineOne || "";
            document.getElementById("lineTwo").value = addr.lineTwo || "";

            document.getElementById("postalCode").value = addr.postalCode;

            const citySelect = document.getElementById("citySelect");
            if(citySelect) citySelect.value = addr.cityId;
        }

        card.innerHTML = ` 
 
                    <div class="wrapper-div" onclick="switchPrimary(${addr.id})" style="cursor: pointer;">
                        <div class="col-12 d-flex justify-content-between align-items-center">
                            <div class="col-8 d-flex justify-content-start align-items-center">
                                <div class="me-4">
                                    <input type="radio" name="isPrimary" class="form-check-input address-radio" 
                                        ${addr.isPrimary ? 'checked' : 'disabled'}>
                                </div>
                                <div>
                                    <span class="fs-6 fw-normal"><span>Address 1 : &nbsp;</span>${addr.lineOne}</span><br>
                                    <span class="fs-6 fw-normal"><span>Address 2 : &nbsp;</span>${addr.lineTwo ? addr.lineTwo : "Not set"}</span><br>
                                    <span>${addr.cityName}</span>
                                </div>
                            </div>
                        </div>
                    </div>
         `;
        list.appendChild(card);
    });
}

async function switchPrimary(id) {
    try {
        const response = await fetch(`api/profiles/set-primary-address/${id}`, {method: "PATCH"});
        if (response.ok) {
            await loadAddress();
        }
    } catch (error) {

    }
}

async function loadUserData() {
    try {
        const response = await fetch("api/profiles/user-profile");

        if (response.ok) {
            if (response.redirected) {
                window.location.href = response.url;
                return;
            }
            const data = await response.json();

            document.getElementById("firstName").value = data.user.firstName;
            document.getElementById("lastName").value = data.user.lastName;
            document.getElementById("mobile").value = data.user.mobile;

        } else {
            Notiflix.Notify.failure("Profile Data Loading Failed!", {
                position: "center-top"
            });
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: "center-top"
        });
    }
}

async function loadCheckoutSummary() {
    try {
        const response = await fetch("api/user-carts/load-cart");
        if (response.ok) {
            const data = await response.json();
            const container = document.getElementById("checkout-cart-items");
            if (data.status && data.cartList.length > 0) {
                let subTotal = 0;
                let html = "";
                data.cartList.forEach((cart) => {
                    let total = Number(cart.qty) * parseFloat(cart.price);
                    subTotal += total;
                    html += `
                        <div class="card mb-3 border-0 shadow-sm" id="checkout-item-${cart.cartId}">
                            <div class="row g-0 align-items-center">
                                <div class="col-md-3 p-2">
                                    <img src="${cart.images[0]}"
                                         class="img-fluid rounded-start" alt="Product Image"
                                         style="height: 70px; width: 70px; object-fit: cover;">
                                </div>
                                <div class="col-md-9">
                                    <div class="card-body py-2">
                                        <div class="d-flex justify-content-between align-items-center mb-1">
                                            <h6 class="card-title mb-0 small">${cart.title}</h6>
                                            <button class="btn btn-sm tex" onclick="removeCheckoutItem(${cart.cartId})"><i class="bi bi-x-lg"></i></button>
                                        </div>
                                        <div class="d-flex justify-content-between align-items-center">
                                            <span class="fw-bold small" style="color: #A2BC71FF">$${new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(cart.price)}</span>
                                            <div class="input-group input-group-sm" style="width: 130px;">
                                                <button class="btn btn-outline-dark" onclick="updateCheckoutQty(${cart.cartId}, ${cart.qty - 1})">-</button>
                                                <input type="text" class="form-control text-center" value="${cart.qty}" readonly>
                                                <button class="btn btn-outline-dark" onclick="updateCheckoutQty(${cart.cartId}, ${cart.qty + 1})">+</button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `;
                });
                container.innerHTML = html;
                document.getElementById("checkout-subtotal").innerHTML = "LKR " + new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(subTotal);
                updateCheckoutTotal();
            } else {
                container.innerHTML = '<div class="text-center text-muted py-4">Your cart is empty. <a href="shop.html">Shop now</a></div>';
                document.getElementById("checkout-subtotal").innerHTML = "LKR 0.00";
                updateCheckoutTotal();
            }
        }
    } catch (e) {
        console.error(e);
    }
}

function updateCheckoutTotal() {
    let subtotalText = document.getElementById("checkout-subtotal").innerHTML.replace("LKR ", "").replace(/,/g, "");
    let subtotal = parseFloat(subtotalText) || 0;
    let deliveryText = document.getElementById("checkout-delivery").innerHTML.replace("$", "").replace(/,/g, "");
    let delivery = parseFloat(deliveryText) || 0;
    let total = subtotal + delivery;
    document.getElementById("checkout-total").innerHTML = "LKR " + new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(total);
}

async function removeCheckoutItem(cartId) {
    await removeCartItem(cartId);
    await loadCheckoutSummary();
    await loadCartItems();
}

async function updateCheckoutQty(cartId, newQty) {
    await updateCartItemQuantity(cartId, newQty);
    await loadCheckoutSummary();
    await loadCartItems();
}

async function checkout() {

    const deliveryType = document.getElementById("deliveryPickup");
    if (document.getElementById('deliveryHome').checked) {
        const deliveryType = document.getElementById("deliveryHome");
    }

    let paymentMethod = document.getElementById("payCash");
    if (document.getElementById('payCard').checked) {
        paymentMethod = document.getElementById("payCard");
    }

    let first_name = document.getElementById("firstName");
    let last_name = document.getElementById("lastName");
    let citySelect = document.getElementById("citySelect");
    let lineOne = document.getElementById("lineOne");
    let lineTwo = document.getElementById("lineTwo");
    let postalCode = document.getElementById("postalCode");
    let mobile = document.getElementById("mobile");
    let currentAddressTick = document.getElementById("isCurrentAddress");

    let paymentDetails = {
        isCurrentAddress: currentAddressTick.checked,
        deliveryType : deliveryType.value,
        paymentMethod : paymentMethod.value,
        firstName : first_name.value,
        lastName : last_name.value,
        mobile: mobile.value,
        citySelect : citySelect.value,
        lineOne: lineOne.value,
        lineTwo: lineTwo.value,
        postalCode: postalCode.value,
    };

    console.log(paymentDetails);

    try {
        const response = await fetch("api/checkout/checkout-process", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(paymentDetails)
        });
        const data = await response.json();
        if (data.status) {
            console.log(data);

            Notiflix.Notify.success("Checkout success:", data.message, {position: 'center-top'});
            payhere.startPayment(data.paymentDetails);
        } else {
            Notiflix.Notify.failure(data.message || "Checkout failed", {position: 'center-top'});
            console.error(data)
        }
    } catch (error) {
        Notiflix.Notify.failure("Something went wrong. Please try again.", {position: 'center-top'});
        console.error(error);
    }
}