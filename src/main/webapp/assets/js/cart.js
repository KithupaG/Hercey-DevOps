window.addEventListener("load", async () => {
    try {
        await loadCartItems();
    } finally {
        Notiflix.Loading.remove();
    }
});

async function addToCart(productId, productQty) {
    const response = await fetch("api/user-carts/cart?productId=" + productId + "&qty=" + productQty);

    if (response.ok) {
        const data = await response.json();
        if (data.status) {
            Notiflix.Notify.success(data.message, {position: 'center-top'});
            await loadCartItems();
        } else {
            Notiflix.Notify.failure(data.message, {position: 'center-top'});
        }
    } else {
        Notiflix.Notify.failure("Something went wrong. Please try again.", {position: 'center-top'});
    }
}

async function loadCartItems() {
    try {
        let cartItemContainer = document.getElementById("cart-content");
        if (cartItemContainer) {
            cartItemContainer.innerHTML = "";
        }
        const response = await fetch("api/user-carts/load-cart");
        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                if (cartItemContainer) {
                    let subTotal = 0;
                    let totalQty = 0;
                    data.cartList.forEach((cart) => {
                        let total = Number(cart.qty) * parseFloat(cart.price);
                        subTotal += total;
                        totalQty += Number(cart.qty);

                        cartItemContainer.innerHTML += `
                        <div class="card mb-3 border-0 shadow-sm" id="cart-item-${cart.cartId}">
                <div class="row g-0 align-items-center">
                    <div class="col-md-3 p-2">
                        <img src="${cart.images[0]}"
                             class="img-fluid rounded-start" alt="Product Image"
                             style="height: 80px; width: 80px; object-fit: cover;">
                    </div>
                    <div class="col-md-9">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <h6 class="card-title mb-0">${cart.title}</h6>
                                <button class="btn tex" onclick="removeCartItem(${cart.cartId});"><i class="bi bi-x-lg"></i></button>
                            </div>
                            <div class="d-flex justify-content-between align-items-center">
                                <span class="fw-bold" style="color: #A2BC71FF">$${new Intl.NumberFormat("en-US", {
                            minimumFractionDigits: 2
                        }).format(cart.price)}</span>
                                <div class="input-group input-group-sm w-50">
                                    <button class="btn btn-outline-dark" onclick="updateCartItemQuantity(${cart.cartId}, ${cart.qty - 1})">-</button>
                                    <input type="text" class="form-control text-center" value="${cart.qty}" readonly>
                                    <button class="btn btn-outline-dark" onclick="updateCartItemQuantity(${cart.cartId}, ${cart.qty + 1})">+</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
                        `;
                    });
                    document.getElementById("order-total-quantity").innerHTML = String(totalQty);
                    document.getElementById("shop-icon-badge").innerHTML = String(totalQty);
                    document.getElementById("order-total-amount").innerHTML = "LKR " + new Intl.NumberFormat("en-US", {
                        minimumFractionDigits: 2
                    }).format(subTotal);
                    document.getElementById("nav-cart-price").innerHTML = "LKR " + new Intl.NumberFormat("en-US", {
                        minimumFractionDigits: 2
                    }).format(subTotal);
                    let checkoutBtn = document.getElementById("checkout-btn");
                    if (checkoutBtn) {
                        checkoutBtn.onclick = function () {
                            window.location.href = "checkout.html";
                        };
                    }
                }
            } else {
                if (data.message === "Cart is empty") {
                    console.log(data.message)
                } else {
                    Notiflix.Notify.failure(data.message, {
                        position: 'center-top'
                    });
                }
            }

        } else {
            Notiflix.Notify.failure("Failed to load cart!", {
                position: 'center-top'
            });
        }
    } catch (error) {
        Notiflix.Notify.failure(error.message, {
            position: 'center-top'
        });
    }
}

async function removeCartItem(cartId) {
    const response = await fetch("api/user-carts/remove?cartItemId=" + cartId);
    if (response.ok) {
        const data = await response.json();
        if (data.status) {
            Notiflix.Notify.success(data.message, {position: 'center-top'});
        } else {
            Notiflix.Notify.failure(data.message, {position: 'center-top'});
        }
    } else {
        Notiflix.Notify.failure("Failed to remove item!", {position: 'center-top'});
    }
    await loadCartItems();
}

async function updateCartItemQuantity(cartId, newQty) {
    if (newQty < 1) {
        await removeCartItem(cartId);
        return;
    }
    const response = await fetch("api/user-carts/update-qty?cartItemId=" + cartId + "&qty=" + newQty);
    if (response.ok) {
        const data = await response.json();
        if (data.status) {
            await loadCartItems();
        } else {
            Notiflix.Notify.failure(data.message, {position: 'center-top'});
        }
    } else {
        Notiflix.Notify.failure("Failed to update quantity!", {position: 'center-top'});
    }
}
