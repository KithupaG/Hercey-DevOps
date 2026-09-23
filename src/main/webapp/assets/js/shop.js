// No UI Slider Config

let slider = document.getElementById('slider');
let minInput = document.getElementById('min-price');
let maxInput = document.getElementById('max-price');
let minRangePrice = 100;
let maxRangePrice = 5000;

function initPriceSlider(min, max) {
    minRangePrice = min;
    maxRangePrice = max;
    if (slider.noUiSlider) {
        slider.noUiSlider.destroy();
    }
    noUiSlider.create(slider, {
        start: [min, max],
        connect: true,
        range: {
            'min': [min],
            'max': [max]
        }
    });
    slider.noUiSlider.on('update', function (values, handle) {
        if (handle === 0) {
            minInput.value = Math.round(values[0]);
        } else {
            maxInput.value = Math.round(values[1]);
        }
    });
    minInput.addEventListener('change', function () {
        slider.noUiSlider.set([this.value, null]);
    });
    maxInput.addEventListener('change', function () {
        slider.noUiSlider.set([null, this.value]);
    });
}

// No UI Slider Config End

window.addEventListener("load", async () => {
    Notiflix.Loading.pulse("", {
        clickToClose: false,
        svgColor: '#0284c7'
    });
    try {
        await loadAllCategories();

        const priceRes = await fetch("api/product/product-data");
        if (priceRes.ok) {
            const priceData = await priceRes.json();
            initPriceSlider(Math.floor(priceData.minPrice), Math.ceil(priceData.maxPrice));
        }

        const params = new URLSearchParams(window.location.search);
        const categoryParam = params.get('category');

        if (categoryParam) {
            selectCategoryFromUrl(categoryParam);
            await searchProduct(current_page * product_per_page);
        } else {
            await loadSearchData();
        }
    } finally {
        Notiflix.Loading.remove();
    }
});
document.getElementById("search-button").addEventListener("click", async () => {
    await searchProduct(0);
});

function selectCategoryFromUrl(categoryName) {
    const container = document.getElementById("categoryListBody");
    const labels = container.querySelectorAll("label");

    // Iterate through all labels to find the matching category name
    for (let label of labels) {
        if (label.textContent.trim() === categoryName) {
            const checkboxId = label.getAttribute("for");
            const checkbox = document.getElementById(checkboxId);
            if (checkbox) {
                checkbox.checked = true;
            }
            break; // Stop looking once found
        }
    }
}

// Working
async function loadAllCategories() {
    const response = await fetch("api/data/categories");

    if (response.ok) {
        const data = await response.json();

        if (data.success) {
            const container = document.getElementById("categoryListBody");
            container.innerHTML = "";

            data.categories.forEach(category => {

                const categoryListItem = document.createElement("li");
                categoryListItem.classList.add("d-flex", "gap-2", "align-items-center");

                categoryListItem.innerHTML = `
                    <input type="checkbox" id="${category.id}" class="form-check-input">
                    <label for="${category.id}" class="form-check-label">${category.name}</label>
                `;

                container.appendChild(categoryListItem);
            });
        }
    }
}

async function loadSearchData() {
    try {
        const response = await fetch("api/product/product-data");
        if (response.ok) {
            const data = await response.json();
            console.log(data);
            updateProductView(data);
        } else {
            Notiflix.Notify.failure("Data loading failed!", {
                position: 'center-top'
            });
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: 'center-top'
        });
    }
}

let current_page = 0;
let product_per_page = 8;

function updateProductView(data) {
    const product_container = document.getElementById("product-container");
    product_container.innerHTML = "";

    data.productList.forEach(product => {
        const displayPrice = new Intl.NumberFormat("en-US", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(product.price);

        const badgeColor = product.qty > 10 ? '#8bc34a' : (product.qty > 0 ? '#ff9800' : '#f44336');
        const availabilityText = product.qty > 0 ? product.qty : "Out of Stock";

        const imageSrc = (product.images && product.images.length > 0)
            ? product.images[0]
            : 'assets/images/default-placeholder.png';

        const cardHtml = `
            <div class="col">
                <div class="card h-100 product-card-hover border border-dark-subtle" onclick="loadSingleProduct(${product.productId})">
                    <img src="${imageSrc}" onclick="redirectToSingleProductView(${product.productId})" class="card-img-top custom-card-img-shop" alt="${product.title}">
                    <div class="card-body bg-body-tertiary">
                        <h5 class="card-title text-truncate">${product.title}</h5>
                        <div class="d-flex justify-content-between align-items-center">
                            <span class="card-text fw-bold">LKR ${displayPrice}</span>
                            <small>
                                <span class="badge" style="background-color: ${badgeColor}">
                                    ${availabilityText}
                                </span>
                            </small>
                        </div>
                        <div class="d-flex justify-content-between align-items-center mt-3">
                            <div>
                                <button type="button" class="btn btn-outline-dark" 
                                    ${product.qty <= 0 ? 'disabled' : ''} 
                                    onclick="addToCart(${product.productId}, 1)">
                                    + <i class="bi bi-cart3"></i>
                                </button>
                            </div>
                            <button type="button" 
                                class="btn ${product.qty <= 0 ? 'btn-secondary disabled' : ''}" 
                                style="${product.qty > 0 ? 'background-color: #A2BC71; color: white' : ''}">
                                ${product.qty > 0 ? 'Buy now' : 'Sold Out'}
                            </button>
                        </div>
                    </div>
                </div>
            </div>`;
        product_container.innerHTML += cardHtml;
    });

    let st_pagination_container = document.getElementById("store-pagination");
    st_pagination_container.innerHTML = "";
    let all_product_count = data.allProductCount;
    document.getElementById("result-count").innerHTML = "Showing " + all_product_count + " results";


    let pages = Math.ceil(all_product_count / product_per_page);

    if (current_page > 0) { // Changed from !== 0 for safety
        let li = document.createElement("li");
        li.className = "page-item";
        li.innerHTML = `<a class="page-link" href="#">Previous</a>`;
        li.addEventListener("click", async (e) => {
            e.preventDefault();
            current_page--;
            await searchProduct(current_page * product_per_page);
        });
        st_pagination_container.appendChild(li);
    }

    for (let i = 0; i < pages; i++) {
        let li = document.createElement("li");
        // Add 'active' class if it's the current page
        li.className = (i === current_page) ? "page-item active" : "page-item";

        let a = document.createElement("a");
        a.className = "page-link";
        a.href = "#";
        a.innerHTML = i + 1;

        li.appendChild(a);

        li.addEventListener("click", async (e) => {
            e.preventDefault();
            current_page = i;
            await searchProduct(i * product_per_page);
        });

        st_pagination_container.appendChild(li);
    }

    if (current_page < (pages - 1)) {
        let li = document.createElement("li");
        li.className = "page-item";
        li.innerHTML = `<a class="page-link" href="#">Next</a>`;
        li.addEventListener("click", async (e) => {
            e.preventDefault();
            current_page++;
            await searchProduct(current_page * product_per_page);
        });
        st_pagination_container.appendChild(li);
    }
}

async function searchProduct(firstResult) {
    Notiflix.Loading.pulse("", {
        clickToClose: false,
        svgColor: '#0284c7'
    });

    // 1. Get the Search Input Value
    const searchInput = document.getElementById("search-input");
    const searchQuery = searchInput ? searchInput.value.trim() : "";

    const selectedCategories = Array.from(document.querySelectorAll('#categoryListBody input[type="checkbox"]:checked'))
        .map(cb => {
            const label = document.querySelector(`label[for="${cb.id}"]`);
            return label ? label.textContent.trim() : cb.id;
        });

    const availabilityInput = document.querySelector('input[name="availability"]:checked');
    const selectedAvailability = availabilityInput
        ? document.querySelector(`label[for="${availabilityInput.id}"]`).textContent.trim()
        : null;

    const price_range_start = maxInput.value;
    const price_range_end = minInput.value;
    const sort_value = document.getElementById("st-sort").value;

    const data = {
        firstResult: firstResult,
        searchQuery: searchQuery, // 2. Add the query to the data object
        categories: selectedCategories,
        availability: selectedAvailability,
        priceStart: price_range_start,
        priceEnd: price_range_end,
        sortValue: sort_value
    };

    console.log(data); // Debugging

    const dataJSON = JSON.stringify(data);
    try {
        const response = await fetch("api/product/advanced-search", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: dataJSON
        });
        if (response.ok) {
            const data = await response.json();
            updateProductView(data);
        } else {
            Notiflix.Notify.failure("Product data loading failed!", {
                position: 'center-top'
            });
        }
    } catch (e) {
        Notiflix.Notify.failure(e.message, {
            position: 'center-top'
        });
    } finally {
        Notiflix.Loading.remove();
    }
}

document.getElementById("apply-filters-button").addEventListener("click", async ()=>{
    try {
        await searchProduct(current_page * product_per_page);
    }catch (e){
        console.log(e);
    }
});

function redirectToSingleProductView(productId){
    window.location = `single-product-view.html?id=${productId}`
}