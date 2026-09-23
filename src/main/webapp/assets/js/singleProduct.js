let params = new URLSearchParams(window.location.search);
window.addEventListener("load",async ()=>{
    await loadSingleProduct(params.get("id"));
});

let imageIconContainer = document.getElementById("image-icon-container");

async function loadSingleProduct(productID){
    const response = await fetch(`api/product/single-product?id=${productID}`, {method:"GET"});
    if (response.ok) {
        const data = await response.json();
        console.log(data);
        document.getElementById("productTitle").innerHTML = `${data.title}`;
        document.getElementById("productPrice").innerHTML = `LKR ${data.stockList[0].price}`;
        document.getElementById("productDescription").innerHTML = `${data.description}`;
        if (data.stockList[0].quantity <= 0){
            document.getElementById("qty").innerHTML= ``;
            document.getElementById("productPrice").style = "color: #a5a5a5";
            document.getElementById("buyNowButton").classList.add("d-none");
            document.getElementById("addToCart").classList.add("d-none");
            document.getElementById("availabilityBadge").style = "background-color: #FF001AFF";
        }
        document.getElementById("qty").innerHTML= `${data.stockList[0].quantity}`;

        const thumbnailContainer = document.getElementById("thumbnail-container");
        const mainImage = document.getElementById("main-image");

        thumbnailContainer.innerHTML = "";
        if (data.images && data.images.length > 0) {
            data.images.forEach((imgUrl, index) => {
                const imgDiv = document.createElement("div");
                imgDiv.classList.add("img-icon", "bg-secondary-subtle", "rounded-3", "m-2", "border");
                imgDiv.style.backgroundImage = `url('${imgUrl}')`;
                imgDiv.onclick = function() {
                    mainImage.style.backgroundImage = `url('${imgUrl}')`;
                };
                thumbnailContainer.appendChild(imgDiv);
                if (index === 0) {
                    mainImage.style.backgroundImage = `url('${imgUrl}')`;
                }
            });
        }else {
            mainImage.innerHTML = "No Image";
        }



    } else {
        console.log("Error:", response.status);
    }
}

function toggleDescription() {
    const container = document.getElementById('desc-container');
    const fadeOverlay = document.getElementById('desc-fade-overlay');
    const btn = document.getElementById('toggleDescBtn');

    // The height you want when collapsed (must match CSS)
    const collapsedHeight = "200px";

    if (container.style.maxHeight === collapsedHeight || container.style.maxHeight === "") {
        // EXPAND: Set max-height to the scrollHeight (actual height of content)
        container.style.maxHeight = container.scrollHeight + "px";
        fadeOverlay.classList.add('fade-hidden');
        btn.innerText = "Show Less";
    } else {
        // COLLAPSE: Set max-height back to fixed pixel amount
        container.style.maxHeight = collapsedHeight;
        fadeOverlay.classList.remove('fade-hidden');
        btn.innerText = "Read More";
    }
}

function changeQty(amount) {
    const qtyInput = document.getElementById("qtyInput");
    let currentQty = parseInt(qtyInput.value);

    let newQty = currentQty + amount;

    // Minimum quantity check
    if (newQty >= 1) {
        qtyInput.value = newQty;
    }
}