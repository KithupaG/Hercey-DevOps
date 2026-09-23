window.addEventListener("load", async () => {
    Notiflix.Loading.standard("", {
        clickToClose: false, svgColor: '#0284c7'
    });
    try {
        await loadAllCategories();
        await loadTeaGallery();
        await loadFreshDeals();
    } finally {
        Notiflix.Loading.remove();
    }
});

async function loadAllCategories() {
    const response = await fetch("api/data/categories");

    if (response.ok) {
        const data = await response.json();

        if (data.success) {
            const container = document.getElementById("category-container");
            container.innerHTML = " "; // Clear the loading text

            data.categories.forEach(category => {
                const categoryDiv = document.createElement("div");

                categoryDiv.className = "card shadow-sm border rounded-3 d-flex flex-column align-items-center justify-content-center p-3 bg-white";

                categoryDiv.style.width = "120px";
                categoryDiv.style.height = "120px";
                categoryDiv.style.cursor = "pointer";
                categoryDiv.style.transition = "all 0.3s ease";

                categoryDiv.onmouseover = () => categoryDiv.classList.add("custom-green-glow");
                categoryDiv.onmouseout = () => categoryDiv.classList.remove("custom-green-glow");

                categoryDiv.onclick = () => {
                    // Encode the name to handle spaces or special characters safely
                    window.location.href = `shop.html?category=${encodeURIComponent(category.name)}`;
                };

                categoryDiv.innerHTML = `
        <div class="mb-2" style="height: 50px; display: flex; align-items: center;">
            <img src="${category.icon}" alt="${category.name}" style="max-width: 45px; max-height: 45px; object-fit: contain;">
        </div>
        <span class="small fw-bold text-dark text-center">${category.name}</span>
    `;

                container.appendChild(categoryDiv);
            });
        }
    }
}

async function loadTeaGallery() {
    try {
        const response = await fetch("api/data/tea-gallery", { method: "GET" });
        const teaData = await response.json();

        const slider = document.getElementById('teaSlider');
        slider.innerHTML = '';

        teaData.forEach(item => {

            const tempDiv = document.createElement("div");
            tempDiv.innerHTML = item.description;
            const plainText = tempDiv.textContent || tempDiv.innerText || "";


            const words = plainText.split(/\s+/);
            const shortDesc = words.slice(0, 15).join(" ") + (words.length > 30 ? "..." : "");

            const cardHtml = `
                <div class="tea-card-item" onclick="redirectToSingleProductView(${item.id})" style="cursor: pointer;">
                    <div class="card shadow-sm">
                        <img src="${item.image}" alt="${item.name}">
                        <div class="card-body text-center">
                            <h5 class="text-start">${item.name}</h5>
                            <h5 class="text-start text-success">${item.price}/-</h5>
                            <p class="small text-muted">${shortDesc}</p>
                            <button class="btn btn-outline-dark btn-sm" onclick="event.stopPropagation(); redirectToSingleProductView(${item.id})">shop</button>
                        </div>
                    </div>
                </div>`;
            slider.insertAdjacentHTML('beforeend', cardHtml);
        });
    } catch (error) {
        console.error("Failed to load gallery:", error);
    }
}

async function loadFreshDeals(){
    try {
        const response = await fetch("api/data/fresh-deals");
        if (response.ok) {
            const data = await response.json();
            renderFreshDeals(data.newArrivals);
        } else {
            Notiflix.Notify.failure("Product data loading failed!", {
                position: 'center-top'
            });
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: 'center-top'
        });
    }
}

function renderFreshDeals(productList) {
    const container = document.querySelector(".fresh-offers-container");
    if (!container) return;

    container.innerHTML = "";

    productList.forEach(product => {
        const stockInfo = product.stockDTOList[0] || { price: 0, qty: 0 };
        const displayPrice = new Intl.NumberFormat("en-US", {
            minimumFractionDigits: 2
        }).format(stockInfo.price);

        // Logic for dynamic badge color
        // If qty > 10 Green, If qty > 0 Orange, Else Red
        const badgeColor = stockInfo.qty > 10 ? '#8bc34a' : (stockInfo.qty > 0 ? '#ff9800' : '#f44336');
        const availabilityText = stockInfo.qty > 0 ? stockInfo.qty : "Out of Stock";

        const cardHtml = `
            <div class="col">
                <div class="card h-100 product-card-hover border border-dark-subtle" >
                    <img src="${product.images[0]}" onclick="redirectToSingleProductView(${product.productId})" class="card-img-top custom-card-img" alt="${product.title}">
                    <div class="card-body bg-body-tertiary">
                        <h5 class="card-title text-truncate">${product.title}</h5>
                        <div class="d-flex justify-content-between align-items-center">
                            <span class="card-text fw-bold">LKR ${displayPrice}</span>
                            <small><span class="badge" style="background-color: ${badgeColor}">${availabilityText}</span></small>
                        </div>
                        <div class="d-flex justify-content-between align-items-center mt-3">
                            <div>
                                <button type="button" class="btn btn-outline-dark" onclick="addToCart(${product.productId},1)" ${stockInfo.qty <= 0 ? 'disabled' : ''}>
                                    <i class="bi bi-cart3"></i>
                                </button>
                                <input type="number" name="" id="qtyInput" value="1" hidden="hidden">
                            </div>
                            <button type="button" class="btn ${stockInfo.qty <= 0 ? 'btn-secondary disabled' : ''}" 
                                    style="${stockInfo.qty > 0 ? 'background-color: #A2BC71; color: white' : ''}">
                                ${stockInfo.qty > 0 ? 'Buy now' : 'Sold Out'}
                            </button>
                        </div>
                    </div>
                </div>
            </div>`;


        container.innerHTML += cardHtml;
    });
}

function redirectToSingleProductView(productId){
    window.location = `single-product-view.html?id=${productId}`
}

function moveSlide(direction) {
    const slider = document.getElementById('teaSlider');
    const cardWidth = document.querySelector('.tea-card-item').offsetWidth + 20; // width + gap

    // Scroll the container left or right
    slider.scrollBy({
        left: direction * cardWidth,
        behavior: 'smooth'
    });
}



