
// Working AF
window.addEventListener("load", async () => {
    try {
        await getCities();
        await loadUserData();
    }catch (e){
        console.log(e);
    }
});
document.addEventListener("DOMContentLoaded", () => {
    const menuLinks = document.querySelectorAll('.menu-link');
    const sections = document.querySelectorAll('.content-section');

    menuLinks.forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault();

            const targetId = this.getAttribute('data-target');
            const targetSection = document.getElementById(targetId);

            if (!targetSection) return;

            sections.forEach(section => {
                section.classList.add('d-none');
            });

            targetSection.classList.remove('d-none');

            menuLinks.forEach(nav => nav.classList.remove('active-nav'));
            this.classList.add('active-nav');
        });
    });
});
document.getElementById("address-anchor").addEventListener("click", async () => {
    await loadAddress();
});
document.getElementById("account-anchor").addEventListener("click", async () => {
    await loadUserData();
});
document.getElementById("product-anchor").addEventListener("click", async () => {
    await populateCategories();
});
document.getElementById("seller-anchor").addEventListener("click", async () => {
    await getSellerDataAndStatus();
});
document.getElementById("shipments-anchor").addEventListener("click", async () => {
    await loadShipments();
});
document.querySelector('a[data-target="productListing"]').addEventListener("click", async () => {
    await loadSellerProducts();
});


// User and Addresses section

async function getCities() {
    try {
        const response = await fetch("api/data/cities")
        if (response.ok) {
            const data = await response.json();
            const citySelect = document.getElementById("citySelect");
            const shopCitySelect = document.getElementById("shopCitySelect");
            const shopCitySelect2 = document.getElementById("shopCitySelect2");
            data.cities.forEach((city) => {
                const option = document.createElement("option");
                option.value = city.id;
                option.innerHTML = city.name;

                // Append the original to the first one
                if (citySelect) citySelect.appendChild(option);

                // Append CLONES to the others
                // .cloneNode(true) creates a deep copy of the element
                if (shopCitySelect) shopCitySelect.appendChild(option.cloneNode(true));
                if (shopCitySelect2) shopCitySelect2.appendChild(option.cloneNode(true));
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

async function loadAddress() {
    Notiflix.Loading.standard("Loading addresses...");
    try {
        const response = await fetch("api/profiles/addresses");
        if (response.ok) {
            const data = await response.json();
            document.getElementById("addName").innerHTML = `${data.name}`;
            renderAddresses(data.addresses);
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: "center-top"
        });
        console.log(e);
    } finally {
        Notiflix.Loading.remove();
    }
}

function renderAddresses(addresses) {
    const list = document.getElementById("addressList");
    const inputField = document.createElement('address-radio');
    list.innerHTML = "";
    addresses.forEach(addr => {
        const card = document.createElement("address-body");
        card.className = "card shadow-sm p-3 mb-3 " + (addr.isPrimary ? "border border-primary-subtle" : "");
        inputField.setAttribute('checked', (addr.isPrimary ? 'checked' : 'un-checked'));
        card.style.backgroundColor = (addr.isPrimary ? "#f8fbff" : "");
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
                            <div>
                                <button class="btn" type="button" onclick="deleteAddress(${addr.id});" style="background-color: #fd6060; color: white"><i
                                        class="bi bi-trash-fill"></i></button>
                            </div>
                        </div>
                    </div>
         `;

        list.appendChild(card);
    });
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

            document.getElementById("username").innerHTML = `Hello, ${data.user.firstName} ${data.user.lastName}`;

            let replacedText = String(data.user.sinceAt).replace("-", " ");

            let since = replacedText.split(" ");
            document.getElementById("since").innerHTML = `Hercey Member Since ${since[1]} ${since[0]}`;
            document.getElementById("firstName").value = data.user.firstName;
            document.getElementById("lastName").value = data.user.lastName;
            document.getElementById("lineOne").value = data.user.lineOne ? data.user.lineOne : "";
            document.getElementById("lineTwo").value = data.user.lineTwo ? data.user.lineTwo : "";
            document.getElementById("postalCode").value = data.user.postalCode ? data.user.postalCode : "";
            document.getElementById("citySelect").value = data.user.cityId ? data.user.cityId : 0;
            document.getElementById("mobile").value = data.user.mobile;
            document.getElementById("currentPassword").value = data.user.password;

            if (!data.isSeller) {
                const productListingLink = document.querySelector('a[data-target="productListing"]');
                const productListingSection = document.getElementById("productListing");
                if (productListingLink) productListingLink.classList.add("d-none");
                if (productListingSection) productListingSection.classList.add("d-none");

                const shipmentsLink = document.querySelector('a[data-target="shipments"]');
                const shipmentsSection = document.getElementById("shipments");
                if (shipmentsLink) shipmentsLink.classList.add("d-none");
                if (shipmentsSection) shipmentsSection.classList.add("d-none");
            }
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

async function getSellerDataAndStatus() {
    const response = await fetch("api/seller/seller-status");
    if (response.ok) {
        const data = await response.json();

        if (data.status) {

            if (data.sellerStatus === "APPROVED") {
                const accountForm = document.getElementById("sellerAccount");
                accountForm.classList.remove("d-none");
                accountForm.classList.add("d-block");

                document.getElementById("sellerEmail2").value = data.seller.email;
                document.getElementById("companyName2").value = data.seller.name;
                document.getElementById("companyMobile2").value = data.seller.mobile;
                document.getElementById("sellerSince").innerHTML = data.seller.since;
                document.getElementById("shopLineOne2").value = data.seller.lineOne || "";
                document.getElementById("shopLineTwo2").value = data.seller.lineTwo || "";
                document.getElementById("shopPostalCode2").value = data.seller.postalCode || "";
                if (data.seller.cityId) {
                    document.getElementById("shopCitySelect2").value = data.seller.cityId;
                }
            }

            if (data.sellerStatus === "PENDING") {
                const pendingDiv = document.getElementById("sellerPending");
                if(pendingDiv){
                    pendingDiv.classList.remove("d-none");
                    pendingDiv.classList.add("d-block");
                }
            }

        } else {
            const noAccountDiv = document.getElementById("noSellerAccount");
            if(noAccountDiv) {
                noAccountDiv.classList.remove("d-none");
            }
        }
    }
}

async function applyForSeller() {
    // 1. Get Elements
    let sellerEmail = document.getElementById("sellerEmail");
    let companyName = document.getElementById("companyName");
    let companyMobile = document.getElementById("companyMobile");
    let lineOne = document.getElementById("shopLineOne");
    let lineTwo = document.getElementById("shopLineTwo");
    let postalCode = document.getElementById("shopPostalCode");
    let city = document.getElementById("shopCitySelect");

    const sellerObject = {
        email: sellerEmail.value,
        companyName: companyName.value,
        companyMobile: companyMobile.value,
        lineOne: lineOne.value,
        lineTwo: lineTwo.value,
        postalCode: postalCode.value,
        cityId: city.value === "" ? 0 : parseInt(city.value)
    };

    Notiflix.Loading.standard('Processing your application...');

    try {
        const response = await fetch("api/seller/apply-for-seller", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(sellerObject)
        });

        Notiflix.Loading.remove();

        if (response.ok) {
            const data = await response.json();

            if (data.status) {
                Notiflix.Report.success(
                    'Application Sent',
                    data.message,
                    'Okay',
                    () => {
                        location.reload();
                    }
                );
            } else {
                Notiflix.Notify.failure(data.message);
            }
        } else {
            Notiflix.Notify.failure("Server error. Please try again later.");
        }

    } catch (error) {
        Notiflix.Loading.remove();
        console.error("Fetch error:", error);
        Notiflix.Notify.failure("Could not connect to the server.");
    }
}

async function updateSeller() {
    Notiflix.Loading.standard("Updating Profile...");

    const sellerDTO = {
        companyEmail: document.getElementById("sellerEmail2").value,
        companyName: document.getElementById("companyName2").value,
        companyMobile: document.getElementById("companyMobile2").value,
        lineOne: document.getElementById("shopLineOne2").value,
        lineTwo: document.getElementById("shopLineTwo2").value,
        postalCode: document.getElementById("shopPostalCode2").value,
        cityId: parseInt(document.getElementById("shopCitySelect2").value) || 0
    };

    try {
        const response = await fetch("api/seller/update-seller", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(sellerDTO)
        });

        const data = await response.json();
        Notiflix.Loading.remove();

        if (response.ok && data.status) {
            Notiflix.Notify.success(data.message);
        } else {
            Notiflix.Notify.failure(data.message || "Update failed");
        }
    } catch (error) {
        Notiflix.Loading.remove();
        console.error(error);
        Notiflix.Notify.failure("System Error");
    }
}

function toggleNAtoApply(){
    document.getElementById("noSellerAccount").classList.toggle("d-none");
    document.getElementById("sellerAccountApply").classList.toggle("d-none");
}

async function saveChanges() {
    Notiflix.Loading.standard("Saving Changes...", {
        clickToClose: false,
        svgColor: "#0284c7"
    });

    let firstName = document.getElementById("firstName");
    let lastName = document.getElementById("lastName");
    let lineOne = document.getElementById("lineOne");
    let lineTwo = document.getElementById("lineTwo");
    let postalCode = document.getElementById("postalCode");
    let citySelect = document.getElementById("citySelect");
    let mobile = document.getElementById("mobile");

    const userObj = {
        firstName: firstName.value,
        lastName: lastName.value,
        lineOne: lineOne.value,
        lineTwo: lineTwo.value,
        postalCode: postalCode.value,
        cityId: citySelect.value === "" ? 0 : parseInt(citySelect.value),
        mobile: mobile.value,
    }
    try {
        const response = await fetch("api/profiles/update-profile", {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(userObj)
        });

        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                Notiflix.Report.success(
                    'Hercey',
                    data.message,
                    'Okay',//button title
                    () => {
                        window.location.reload();
                    },
                );
                console.log("fetch for update profile success")
                await loadUserData();
            } else {
                Notiflix.Notify.failure(data.message, {
                    position: "center-top"
                });
            }

        } else {
            Notiflix.Notify.failure("Profile Update Failed!", {
                position: "center-top"
            });
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: "center-top"
        });
    } finally {
        Notiflix.Loading.remove(1000);
    }
}

async function updatePassword() {
    Notiflix.Loading.standard("Saving Changes...", {
        clickToClose: false,
        svgColor: "#0284c7"
    });

    let currentPassword = document.getElementById("currentPassword");
    let newPassword = document.getElementById("newPassword");
    let confirmPassword = document.getElementById("confirmPassword");

    const userObj = {
        password: currentPassword.value,
        newPassword: newPassword.value,
        confirmPassword: confirmPassword.value
    }

    try {
        const response = await fetch("api/profiles/update-password", {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(userObj)
        });

        if (response.ok) {
            const data = await response.json();
            if (data.status) {
                Notiflix.Report.success(
                    'Hercey',
                    data.message,
                    'Okay',//button title
                    () => {
                        window.location.reload();
                    },
                );
                console.log("fetch for update profile success")
                await loadUserData();
            } else {
                Notiflix.Notify.failure(data.message, {
                    position: "center-top"
                });
            }

        } else {
            Notiflix.Notify.failure("Profile Update Failed!", {
                position: "center-top"
            });
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: "center-top"
        });
    } finally {
        Notiflix.Loading.remove(1000);
    }
}

async function deleteAddress(id) {
    Notiflix.Confirm.show(
        'Delete Address',
        'Are you sure you want to delete this address? This action cannot be undone.',
        'Yes',
        'No',
        async () => {
            // This runs if the user clicks "Yes"
            try {
                const response = await fetch(`api/profiles/delete-address/${id}`, {
                    method: "DELETE",
                });

                if (response.ok) {
                    Notiflix.Notify.success('Address deleted successfully');
                    loadAddress();
                }
            } catch (error) {
                console.error("Failed to delete address:", error);
                Notiflix.Report.failure(
                    'Delete Failed',
                    'There was an error deleting the address. Please try again.',
                    'Okay'
                );
            }
        },
        () => {
            // This runs if the user clicks "No"
            console.log("Deletion cancelled");
        },
        {
            // Optional styling to make the "Yes" button look like a danger button
            okButtonBackground: '#ff5555',
            titleColor: '#1e1e1e',
        }
    );
}

async function switchPrimary(id) {
    try {
        const response = await fetch(`api/profiles/set-primary-address/${id}`, {method: "PATCH"});
        if (response.ok) {
            loadAddress();
        }
    } catch (error) {

    }
}

// Product Section

async function populateCategories() {
    const response = await fetch("api/data/categories");

    if (response.ok) {
        const data = await response.json();

        if (data.success) {
            const productCategorySelector = document.getElementById("CategorySelect");

            // 1. Map the categories to HTML option strings
            // 2. Join them into one large string
            const optionsHTML = data.categories.map(category => {
                return `<option value="${category.id}">${category.name}</option>`;
            }).join("");

            // 3. Update the innerHTML once
            productCategorySelector.innerHTML = optionsHTML;
        }
    }
}

async function addProduct() {
    Notiflix.Loading.standard("Checking permissions...", {
        clickToClose: false,
        svgColor: "#0284c7"
    });

    try {
        const statusResponse = await fetch("api/seller/seller-status");

        if (statusResponse.ok) {
            const statusData = await statusResponse.json();

            if (statusData.sellerStatus === "APPROVED") {

                Notiflix.Loading.pulse("Uploading Product...");

                const title = document.getElementById("productTitle");
                const categorySelect = document.getElementById("CategorySelect");
                const descriptionContent = quill.root.innerHTML;
                const length = document.getElementById("length");
                const width = document.getElementById("width");
                const height = document.getElementById("height");
                const weight = document.getElementById("weight");
                const mfd = document.getElementById("mfd");
                const epd = document.getElementById("epd");

                const productData = {
                    title: title.value,
                    category: parseInt(categorySelect.value),
                    price: parseFloat(document.getElementById("unitPrice").value),
                    qty: parseInt(document.getElementById("stock").value),
                    description: descriptionContent,
                    length : length.value,
                    width : width.value,
                    height : height.value,
                    weight : weight.value,
                };

                const mfd_epd = {
                    manufacturedDate : mfd.value,
                    expirationDate : epd.value
                }

                const formData = new FormData();
                formData.append("product", JSON.stringify(productData));
                formData.append("mfd_epd", JSON.stringify(mfd_epd) )

                for (let i = 1; i <= 4; i++) {
                    const fileInput = document.getElementById(`image${i}`);
                    if (fileInput && fileInput.files.length > 0) {
                        formData.append("images", fileInput.files[0]);
                    }
                }

                const productResponse = await fetch("api/product/addProduct", {
                    method: "POST",
                    body: formData
                });

                const result = await productResponse.json();

                Notiflix.Loading.remove();

                if (productResponse.ok && result.status) {
                    Notiflix.Notify.success("Product added successfully!");
                    resetProductForm();
                } else {
                    Notiflix.Notify.failure(result.message || "Failed to add product.");
                }

            } else {
                Notiflix.Loading.remove();
                Notiflix.Report.warning("Access Denied", "Your seller account must be APPROVED to add products.", "Okay");
            }
        } else {
            Notiflix.Loading.remove();
            Notiflix.Notify.failure("Could not verify seller status.");
        }

    } catch (error) {
        Notiflix.Loading.remove();
        console.error("Error:", error);
        Notiflix.Notify.failure("An unexpected error occurred.");
    }
}

function resetProductForm() {
    document.getElementById("productTitle").value = "";
    document.getElementById("unitPrice").value = "";
    document.getElementById("stock").value = "";
    document.getElementById("length").value = "";
    document.getElementById("width").value = "";
    document.getElementById("height").value = "";
    document.getElementById("weight").value = "";
    document.getElementById("mfd").value = "";
    document.getElementById("epd").value = "";
    document.getElementById("CategorySelect").selectedIndex = 0;
    quill.root.innerHTML = "";
    for (let i = 1; i <= 4; i++) {
        const fileInput = document.getElementById(`image${i}`);
        const imageDiv = document.getElementById(`image${i}div`);
        const span = imageDiv.querySelector('span');
        const icon = imageDiv.querySelector('i');
        clearImage(fileInput, imageDiv, span, icon);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    for (let i = 1; i <= 4; i++) {
        const fileInput = document.getElementById(`image${i}`);
        const imageDiv = document.getElementById(`image${i}div`);
        const span = imageDiv.querySelector('span');
        const icon = imageDiv.querySelector('i');

        imageDiv.addEventListener('click', () => {
            if (fileInput.files.length === 0) {
                fileInput.click();
            } else {
                clearImage(fileInput, imageDiv, span, icon);
            }
        });

        // CHANGE LOGIC (When file is picked)
        fileInput.addEventListener('change', function() {
            const file = this.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    imageDiv.style.backgroundImage = `url(${e.target.result})`;
                    imageDiv.classList.add('has-image'); // Helper class for CSS

                    // Update icon to trash
                    icon.className = 'bi bi-trash fs-4 text-danger';
                    span.style.display = 'none';
                };
                reader.readAsDataURL(file);
            }
        });
    }
});

function clearImage(input, div, span, icon) {
    input.value = ""; // Clear file
    div.style.backgroundImage = 'none';
    div.classList.remove('has-image');
    span.style.display = 'block';
    icon.className = 'bi bi-upload fs-4 text-secondary';
}

// Shipments Section

async function loadShipments() {
    const container = document.getElementById("shipmentsContainer");
    if (!container) return;

    Notiflix.Loading.standard("Loading shipments...");

    try {
        const response = await fetch("api/seller/shipments");

        if (!response.ok) {
            Notiflix.Loading.remove();
            Notiflix.Notify.failure("Failed to load shipments.");
            return;
        }

        const data = await response.json();

        Notiflix.Loading.remove();

        if (!data.status || !data.shipments || data.shipments.length === 0) {
            container.innerHTML = `<div class="text-center py-5 text-muted"><h5>No shipments found.</h5></div>`;
            return;
        }

        let html = "";
        for (const order of data.shipments) {
            html += `
                <div class="border rounded mb-4 p-3" style="border-color: #e0e0e0 !important;">
                    <div class="d-flex flex-wrap justify-content-between align-items-center mb-3">
                        <div>
                            <strong>Order #${order.orderId}</strong>
                            <span class="ms-3 text-muted">${order.customerName}</span>
                        </div>
                        <div class="text-muted small">
                            Ordered: ${order.orderDate} &nbsp;|&nbsp; Deadline: <span style="color: #d32f2f; font-weight: 600;">${order.deadline}</span>
                        </div>
                    </div>
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <span class="fw-bold">Total: LKR ${order.totalPrice.toFixed(2)}</span>
                    </div>
                    <hr class="my-2">
                    <div class="table-responsive">
                        <table class="table table-borderless mb-0 align-middle">
                            <thead class="table-light">
                                <tr>
                                    <th>Product</th>
                                    <th>Qty</th>
                                    <th>Price</th>
                                    <th>Status</th>
                                    <th>Action</th>
                                </tr>
                            </thead>
                            <tbody>`;
            for (const item of order.items) {
                const imgSrc = item.image ? item.image : "assets/images/placeholder.png";
                html += `
                                <tr>
                                    <td>
                                        <div class="d-flex align-items-center">
                                            <img src="${imgSrc}" alt="${item.productName}" style="width: 50px; height: 50px; object-fit: cover; border-radius: 4px; margin-right: 10px;">
                                            <span>${item.productName}</span>
                                        </div>
                                    </td>
                                    <td>${item.quantity}</td>
                                    <td>LKR ${item.price.toFixed(2)}</td>
                                    <td><span class="badge ${getStatusBadgeClass(item.status)}">${item.status}</span></td>
                                    <td>
                                        <select class="form-select form-select-sm" style="width: auto;" onchange="updateShipmentStatus(${item.itemId}, this)">
                                            <option value="">Change</option>
                                            <option value="PACKING">Packing</option>
                                            <option value="COMPLETED">Complete</option>
                                            <option value="REJECTED">Rejected</option>
                                            <option value="CANCELLED">Cancelled</option>
                                        </select>
                                    </td>
                                </tr>`;
            }
            html += `
                            </tbody>
                        </table>
                    </div>
                </div>`;
        }

        container.innerHTML = html;

    } catch (e) {
        Notiflix.Loading.remove();
        console.error(e);
        Notiflix.Notify.failure("Error loading shipments.");
    }
}

function getStatusBadgeClass(status) {
    switch (status) {
        case "PENDING": return "bg-warning text-dark";
        case "PACKING": return "bg-info text-white";
        case "COMPLETED": return "bg-success";
        case "REJECTED": return "bg-danger";
        case "CANCELLED": return "bg-secondary";
        default: return "bg-secondary";
    }
}

async function updateShipmentStatus(itemId, selectEl) {
    const newStatus = selectEl.value;
    if (!newStatus) return;

    Notiflix.Confirm.show(
        'Update Status',
        `Are you sure you want to change this item status to "${newStatus}"?`,
        'Yes',
        'No',
        async () => {
            Notiflix.Loading.standard("Updating status...");
            try {
                const response = await fetch(`api/seller/shipments/${itemId}/status`, {
                    method: "PATCH",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ status: newStatus })
                });

                const data = await response.json();
                Notiflix.Loading.remove();

                if (response.ok && data.status) {
                    Notiflix.Notify.success(data.message);
                    await loadShipments();
                } else {
                    Notiflix.Notify.failure(data.message || "Update failed.");
                    selectEl.value = "";
                }
            } catch (e) {
                Notiflix.Loading.remove();
                console.error(e);
                Notiflix.Notify.failure("Error updating status.");
                selectEl.value = "";
            }
        },
        () => {
            selectEl.value = "";
        }
    );
}

// Product Listing Section

async function loadSellerProducts() {
    const container = document.getElementById("sellerProductsContainer");
    if (!container) return;

    Notiflix.Loading.standard("Loading products...");

    try {
        const response = await fetch("api/product/seller-products");

        if (!response.ok) {
            Notiflix.Loading.remove();
            Notiflix.Notify.failure("Failed to load products.");
            return;
        }

        const data = await response.json();
        Notiflix.Loading.remove();

        if (!data.status || !data.products || data.products.length === 0) {
            container.innerHTML = `<div class="text-center py-5 text-muted"><h5>No products listed yet.</h5></div>`;
            return;
        }

        let html = `<div class="table-responsive">
            <table class="table table-borderless align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th>Product</th>
                        <th>Category</th>
                        <th>Price</th>
                        <th>Stock</th>
                        <th>Weight</th>
                        <th>Status</th>
                        <th>Listed</th>
                        <th>Updated</th>
                    </tr>
                </thead>
                <tbody>`;

        for (const p of data.products) {
            const imgSrc = p.image ? p.image : "assets/images/placeholder.png";
            const statusBadge = getProductStatusBadge(p.status);
            html += `
                <tr>
                    <td>
                        <div class="d-flex align-items-center">
                            <img src="${imgSrc}" alt="${p.title}" style="width: 50px; height: 50px; object-fit: cover; border-radius: 4px; margin-right: 10px;">
                            <span>${p.title}</span>
                        </div>
                    </td>
                    <td>${p.category}</td>
                    <td>LKR ${p.price.toFixed(2)}</td>
                    <td>${p.qty}</td>
                    <td>${p.weight} kg</td>
                    <td><span class="badge ${statusBadge}">${p.status}</span></td>
                    <td class="text-muted small">${p.createdAt || ""}</td>
                    <td class="text-muted small">${p.updatedAt || ""}</td>
                </tr>`;
        }

        html += `</tbody></table></div>`;
        container.innerHTML = html;

    } catch (e) {
        Notiflix.Loading.remove();
        console.error(e);
        Notiflix.Notify.failure("Error loading products.");
    }
}

function getProductStatusBadge(status) {
    switch (status) {
        case "APPROVED": return "bg-success";
        case "PENDING": return "bg-warning text-dark";
        case "INACTIVE": return "bg-secondary";
        case "REJECTED": return "bg-danger";
        default: return "bg-secondary";
    }
}