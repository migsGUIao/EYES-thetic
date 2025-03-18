/* For the "Upload photo" and "Take a photo" virtual closet feature */

//for upload photo
let resnetModel;

//take photo
let mobilenetModel;
let bodyPixModel = null; // color detection

const topKeywords = ["top", "t-shirt", "tee shirt", "shirt", "jersey", "sweatshirt", "poncho", "sweater",
    "cardigan", "jacket", "trench coat", "kimono", "apron", "cloak","abaya", "bulletproof vest", "vestment"];
const bottomKeywords = ["bottom", "pant", "pants", "jean", "jeans", "blue jeans", "trousers", "shorts", 
       "miniskirt", "mini skirt", "skirt", "mini"];

// upload photo: Load the model once the page loads
window.addEventListener("load", async () => {
    try {
        resnetModel = await tf.loadLayersModel('tfjs_model/model.json'); 
        console.log("Custom ResNet model loaded successfully.");
    } catch (err) {
        console.error("Error loading ResNet model:", err);
    }
});

// take photo: load model
window.addEventListener("load", async () => {
    try {
        mobilenetModel = await mobilenet.load({ version: 2, alpha: 1.0 });
        console.log("MobileNetV2 model loaded.");

        bodyPixModel = await bodyPix.load({
            architecture: 'MobileNetV1',
            outputStride: 16,
            multiplier: 0.75,
            quantBytes: 2
        });
        console.log("BodyPix model loaded.");
    } catch (err) {
        console.error("Error loading models:", err);
    }
});

const viewModal = document.createElement("div");
viewModal.id = "viewModal";
viewModal.className = "fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 hidden z-50";
viewModal.innerHTML = `
    <div class="bg-white p-8 rounded-lg shadow-lg w-full max-w-2xl text-center relative flex">
        <button id="closeViewModal" class="absolute top-4 right-4 text-gray-700 hover:text-black text-3xl">&times;</button>

        <!-- Left: Uploaded Image -->
        <div class="w-1/2 flex justify-center items-center p-4">
            <img id="viewModalImage" src="" alt="Clothing Item" class="max-h-96 rounded-lg shadow-md">
        </div>

        <!-- Right: Clothing Details -->
        <div class="w-1/2 text-left p-4">
            <label class="block mt-2 font-semibold">Name:</label>
            <input type="text" id="viewName" class="w-full border rounded-md p-2 bg-gray-100" disabled>
            
            <label class="block mt-2 font-semibold">Type:</label>
            <select id="viewType" class="w-full border rounded-md p-2 bg-gray-100" disabled>
                <option value="top">Top</option>
                <option value="bottom">Bottom</option>
            </select>

            <label class="block mt-2 font-semibold">Color:</label>
            <input type="text" id="viewColor" class="w-full border rounded-md p-2 bg-gray-100" disabled>

            <h4 class="text-lg font-bold mt-4">Additional Details:</h4>

            <label class="block mt-2 font-semibold">Gender:</label>
            <select id="viewGender" class="w-full border rounded-md p-2 bg-gray-100" disabled>
                <option value="Men">Men</option>
                <option value="Women">Women</option>
            </select>

            <label class="block mt-2 font-semibold">Season:</label>
            <select id="viewSeason" class="w-full border rounded-md p-2 bg-gray-100" disabled>
                <option value="Fall">Fall</option>
                <option value="Summer">Summer</option>
                <option value="Winter">Winter</option>
                <option value="Spring">Spring</option>
            </select>

            <label class="block mt-2 font-semibold">Usage:</label>
            <select id="viewUsage" class="w-full border rounded-md p-2 bg-gray-100" disabled>
                <option value="Casual">Casual</option>
                <option value="Formal">Formal</option>
                <option value="Sports">Sports</option>
            </select>

            <div class="flex space-x-2 mt-4">
                <button id="editDetails" class="bg-yellow-400 text-black font-semibold py-2 px-4 rounded-lg hover:bg-yellow-500 transition" tabindex="0">Edit</button>
                <button id="deleteDetails" class="bg-red-500 text-white font-semibold py-2 px-4 rounded-lg hover:bg-red-600 transition" tabindex="0">Delete</button>
                <button id="saveDetails" class="bg-green-500 text-white font-semibold py-2 px-4 rounded-lg hover:bg-green-600 transition hidden" tabindex="0">Save</button>
            </div>
        </div>
    </div>
`;
document.body.appendChild(viewModal);
document.getElementById("closeViewModal").addEventListener("click", () => {
    viewModal.classList.add("hidden");
});
closeViewModal.addEventListener("keypress", (e) => {
    if (e.key === "Enter") closeViewModal.click();
});
//Upload Photo
document.addEventListener("DOMContentLoaded", function () {
    const uploadBtn = document.getElementById("uploadBtn");
    const fileInput = document.getElementById("fileInput");
    const topsContainer = document.querySelector(".tops-container");
    const bottomsContainer = document.querySelector(".bottoms-container");

    if (!uploadBtn || !fileInput || !photoContainer) {
        console.error("Upload button, file input, or photo container not found! Check closet.html.");
        return; // Stop execution if elements are missing
    }

    uploadBtn.addEventListener("click", function () {
        fileInput.click();
    });

    fileInput.addEventListener("change", function (event) {
        const file = event.target.files[0];

        if (file) {
            const reader = new FileReader();

            reader.onload = function (e) {
                const arrayBuffer = e.target.result;
                const uint8Array = new Uint8Array(arrayBuffer);
                const hex = Array.from(uint8Array.slice(0, 4)) // Read first 4 bytes
                    .map(byte => byte.toString(16).padStart(2, "0"))
                    .join("");

                console.log(`File Signature (Hex): ${hex}`);

                const validSignatures = [
                    "89504e47", // PNG
                    "ffd8ffe0", // JPEG (Standard)
                    "ffd8ffe1", // JPEG (Canon, EXIF)
                    "ffd8ffe2", // JPEG (EXIF)
                    "ffd8ffe3", // JPEG (Samsung)
                    "ffd8ffe8", // JPEG (SPIFF)
                    "52494646"  // WEBP
                ];

                if (!validSignatures.includes(hex)) {
                    alert("Invalid file! Please upload a valid image (PNG, JPG, JPEG, WEBP).");
                    return;
                }

                const imgElement = document.createElement("img");
                imgElement.src = URL.createObjectURL(file);
                imgElement.className = "w-24 h-24 object-cover rounded-md shadow-md";

                // Determine if the uploaded image is a Top or Bottom
                detectCategory(imgElement.src)
                    .then(category => {
                        if (category === "top") {
                            topsContainer.appendChild(imgElement);
                        } else {
                            bottomsContainer.appendChild(imgElement);
                        }
                    })
                    .catch(err => {
                        console.error("Error during classification:", err);
                        // Default to Tops if unsure
                        topsContainer.appendChild(imgElement);
                    });
            };

            reader.readAsArrayBuffer(file);
        }
    });

    console.log("upload.js successfully loaded and event listeners added.");
});


//Take a photo
document.addEventListener("DOMContentLoaded", function () {
    const openCameraBtn = document.getElementById("openCamBtn");
    const closeCameraBtn = document.getElementById("closeCamBtn");
    const cameraModal = document.getElementById("cameraModal");
    const video = document.getElementById("video");
    const canvas = document.getElementById("canvas");
    const captureBtn = document.getElementById("captureBtn");
    const topsContainer = document.querySelector(".tops-container");
    const bottomsContainer = document.querySelector(".bottoms-container");    

    let stream = null; // Store camera stream
    let realTimeInterval;

    // Open the Camera Popup
    openCameraBtn.addEventListener("click", function () {
        cameraModal.classList.remove("hidden");

        // Start webcam stream
        navigator.mediaDevices.getUserMedia({ video: true })
        .then((cameraStream) => {
            stream = cameraStream;
            video.srcObject = stream;
            video.onloadedmetadata = () => {
                video.play();
                // Start real-time detection loop
                realTimeInterval = setInterval(() => {
                    classifyFrame(video);
                }, 500); // Adjust interval (ms) for speed
            };
        })
        .catch((err) => {
            console.error("Error accessing webcam: ", err);
        });
    });

    async function classifyFrame(video) {
        if (!mobilenetModel) return;
        
        const predictions = await mobilenetModel.classify(video);
        console.log(predictions);
    
        const label = predictions[0].className.toLowerCase();
    
        const isRecognized = topKeywords.some(keyword => label.includes(keyword)) ||
                             bottomKeywords.some(keyword => label.includes(keyword));
    
        showDetectionMessage(isRecognized);
    }    
    
    function stopCamera() {
        if (realTimeInterval) {
            clearInterval(realTimeInterval);
            realTimeInterval = null;
        }
    
        if (stream) {
            let tracks = stream.getTracks();
            tracks.forEach(track => track.stop());
            stream = null;
        }
    }

    // Close the Camera Popup
    closeCameraBtn.addEventListener("click", function () {
        cameraModal.classList.add("hidden");
        stopCamera();
    });

    // Capture Photo and Display it
    captureBtn.addEventListener("click", async function () {
        const context = canvas.getContext("2d", { willReadFrequently: true });
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        context.drawImage(video, 0, 0, canvas.width, canvas.height);

        // Create an image element
        const imgSrc = canvas.toDataURL("image/png");
        const detectionMessage = document.getElementById("detectionMessage");

       try {
        const category = await liveDetect(imgSrc); // returns "top" or "bottom"
        console.log(`Category detected: ${category}`);

        const segmentation = await segmentClothing(canvas);
        console.log('Segmentation done');

        const avgColor = getClothingColorWithMask(context, canvas.width, canvas.height, segmentation);
        console.log('Avg Color:', avgColor);

        //const colorName = avgColor ? rgbToColorName(avgColor.r, avgColor.g, avgColor.b) : "unknown";
        let colorName = "unknown";

        //if (avgColor) {
            const hsl = rgbToHsl(avgColor.r, avgColor.g, avgColor.b);
            colorName = hslToColorName(hsl.h, hsl.s, hsl.l);
        
            console.log(`Detected HSL: (${Math.round(hsl.h)}, ${Math.round(hsl.s)}%, ${Math.round(hsl.l)}%) → ${colorName}`);
        //} else {
        //    console.warn('No avgColor returned. Possible segmentation issue.');
        //}

        // Success: Append photo
        const imgElement = document.createElement("img");
        imgElement.src = imgSrc;
        imgElement.className = "w-24 h-24 object-cover rounded-md shadow-md";

        if (category === "top") {
            topsContainer.appendChild(imgElement);
        } else {
            bottomsContainer.appendChild(imgElement);
        }

        // Success: Hide old message
        detectionMessage.classList.add("hidden");
        console.log(`Detected color RGB: (${avgColor.r}, ${avgColor.g}, ${avgColor.b}) → ${colorName}`);
        //speakText(`Photo captured. ${category} detected in ${colorName} color.`);

        document.getElementById("uploadedImagePreview").src = imgSrc;
        document.getElementById("clothingType").value = category;
        document.getElementById("clothingName").value = "";
        document.getElementById("gender").value = "";
        document.getElementById("season").value = "";
        document.getElementById("usage").value = "";
        document.getElementById("clothingColor").value = colorName;

        // Show label modal
        document.getElementById("labelModal").classList.remove("hidden")
        document.getElementById("clothingName").focus();
        
        const closeLabelModal = document.getElementById("closeLabelModal");
        closeLabelModal.addEventListener("click", () => {
            document.getElementById("labelModal").classList.add("hidden");
        });

        closeLabelModal.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                closeLabelModal.click();
            }
        });

        // Close camera modal
        cameraModal.classList.add("hidden");
        stopCamera();

    } catch (err) {
        console.warn("Detection failed. Prompting user to retake.");

        //speakText("Unrecognized item. Please take the photo again.");
        navigator.vibrate([100, 50, 100]);

        detectionMessage.classList.remove("hidden");
    }
    });
    
});

// Segment person/clothing
async function segmentClothing(canvas) {
    const segmentation = await bodyPixModel.segmentPerson(canvas, {
        internalResolution: 'medium',
        segmentationThreshold: 0.7
    });
    return segmentation;
}

// Extract average color from mask
function getClothingColorWithMask(context, width, height, mask) {
    const imageData = context.getImageData(0, 0, width, height);
    const data = imageData.data;
    const maskData = mask.data;

    let r = 0, g = 0, b = 0, count = 0;

    for (let i = 0; i < data.length; i += 4) {
        const maskIndex = i / 4;
        if (maskData[maskIndex] === 1) { // Foreground
            r += data[i];
            g += data[i + 1];
            b += data[i + 2];
            count++;
        }
    }

    if (count === 0) return null;

    r = Math.round(r / count);
    g = Math.round(g / count);
    b = Math.round(b / count);

    return { r, g, b };
}

// Convert RGB to simple color name
function rgbToColorName(r, g, b) {
    // White detection (broad range)
    if (r > 160 && g > 160 && b > 160 && Math.abs(r - g) < 40 && Math.abs(r - b) < 40 && Math.abs(g - b) < 40) return "white";

    // Black detection
    if (r < 60 && g < 60 && b < 60) return "black";

    // Gray detection
    if (Math.abs(r - g) < 20 && Math.abs(r - b) < 20 && r >= 60 && r <= 180) return "gray";

    // Orange shades
    if (r >= 200 && g >= 80 && g <= 150 && b < 100) return "orange";


    // Red shades
    if (r > 150 && g < 100 && b < 100) return "red";

    // Yellow shades
    if (r > 180 && g > 180 && b < 80) return "yellow";

    // Green shades
    if (g > 150 && r < 100 && b < 100) return "green";

    // Blue shades
    if (b > 150 && r < 100 && g < 100) return "blue";

    // Purple shades
    if (r > 100 && b > 100 && g < 100) return "purple";

    return "unknown color";
}


//rgb to hsl 
function rgbToHsl(r, g, b) {
    r /= 255; g /= 255; b /= 255;
    const max = Math.max(r, g, b), min = Math.min(r, g, b);
    let h, s, l = (max + min) / 2;

    if (max === min) {
        h = s = 0; // achromatic (gray)
    } else {
        const d = max - min;
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);

        switch (max) {
            case r: h = (g - b) / d + (g < b ? 6 : 0); break;
            case g: h = (b - r) / d + 2; break;
            case b: h = (r - g) / d + 4; break;
        }
        h /= 6;
    }

    return { h: h * 360, s: s * 100, l: l * 100 };
}

function hslToColorName(h, s, l) {
    if (l > 90 && s < 10) return "white";
    if (l < 12) return "black";
    if (s < 10 && l >= 12 && l <= 90) return "gray";

    // Red range
    if ((h >= 0 && h < 10) || h >= 350) return "red";

    // Orange & Brown (darker orange shades)
    if (h >= 10 && h < 25) {
        if (l < 40) return "brown";
        return "orange";
    }

    // Yellow
    if (h >= 25 && h < 65) return "yellow";

    // Olive/Greenish Yellow
    if (h >= 65 && h < 85) return "olive";

    // Green
    if (h >= 85 && h < 150) return "green";

    // Cyan/Teal
    if (h >= 150 && h < 190) return "cyan";

    // Blue shades
    if (h >= 190 && h < 240) {
        if (l < 30) return "navy";
        return "blue";
    }

    // Purple
    if (h >= 240 && h < 290) return "purple";

    // Pink / Magenta
    if (h >= 290 && h < 330) {
        if (l > 70) return "pink";
        return "magenta";
    }

    // Catch pastel shades, light brown, beige, unknown
    if (s < 20 && l > 70 && l < 90) return "beige";

    return "unknown";
}



async function detectCategory(imgSrc) {
    // Ensure the model is loaded.
    if (!resnetModel) {
      throw new Error("Model is not loaded yet!");
    }
  
    const img = new Image();
    img.crossOrigin = "anonymous"; // in case of CORS issues
    img.src = imgSrc;
  
    await new Promise((resolve, reject) => {
      img.onload = resolve;
      img.onerror = reject;
    });
  
    let tensor = tf.browser.fromPixels(img)
      .resizeNearestNeighbor([224, 224]) // resize to 224x224
      .toFloat()
      .div(tf.scalar(255)); // normalize pixel values between 0 and 1
  
    const batched = tensor.expandDims(0);
  
    const prediction = await resnetModel.predict(batched).data();
  
    // Assume your model outputs probabilities for 2 classes:
    // Index 0 = "top" and index 1 = "bottom" (adjust as needed).
    const predictedIndex = prediction.indexOf(Math.max(...prediction));
    return predictedIndex === 0 ? "top" : "bottom";
}

//take photo: live detect
async function liveDetect(imgSrc) {
    if (!mobilenetModel) {
        throw new Error("Model is not loaded yet!");
    }

    const img = new Image();
    img.crossOrigin = "anonymous";
    img.src = imgSrc;

    await new Promise((resolve, reject) => {
        img.onload = resolve;
        img.onerror = reject;
    });

    const predictions = await mobilenetModel.classify(img);
    console.log(predictions);

    const label = predictions[0].className.toLowerCase();

    // Whitelist filtering
    if (topKeywords.some(keyword => label.includes(keyword))) {
        //speakText(`Detected: ${label}`);
        navigator.vibrate([200]); // Short buzz for success
        return "top";
    } else if (bottomKeywords.some(keyword => label.includes(keyword))) {
        //speakText(`Detected: ${label}`);
        navigator.vibrate([200]);
        return "bottom";
    } else {
        //speakText("Unrecognized item. Please adjust or try again.");
        navigator.vibrate([100, 50, 100]); // Double buzz
        throw new Error("Unrecognized item");
    }
}

// Detection feedback
function showDetectionMessage(isRecognized) {
    const resultElem = document.getElementById("liveResult");
    if (isRecognized) {
        resultElem.textContent = `Accepted as a clothing item.`;
        resultElem.className = "text-green-500 font-bold m-2";
    } else {
        resultElem.textContent = "Unrecognized item. Please try again.";
        resultElem.className = "text-red-500 font-bold m-2";
    }
}

// Save to closet locally
const savePhotoBtn = document.getElementById("savePhoto");
savePhotoBtn.addEventListener("click", function () {
    const clothingName = document.getElementById("clothingName").value.trim();
    const clothingType = document.getElementById("clothingType").value;
    const clothingColor = document.getElementById("clothingColor").value.trim();
    const gender = document.getElementById("gender").value;
    const season = document.getElementById("season").value;
    const usage = document.getElementById("usage").value;
    const imgSrc = document.getElementById("uploadedImagePreview").src;

    if (!clothingName || !clothingType || !clothingColor || !gender || !season || !usage) {
        alert("Please fill in all fields.");
        return;
    }

    const clothingItem = {
        name: clothingName,
        type: clothingType,
        color: clothingColor,
        gender: gender,
        season: season,
        usage: usage,
        imageSrc: imgSrc,
        timestamp: new Date().toISOString()
    };

    // Save locally (e.g., in localStorage or sessionStorage)
    let closet = JSON.parse(localStorage.getItem("virtualCloset")) || [];
    closet.push(clothingItem);
    localStorage.setItem("virtualCloset", JSON.stringify(closet));

    alert("Photo saved to your closet!");
    document.getElementById("labelModal").classList.add("hidden");
    renderCloset();
});

const editBtn = viewModal.querySelector("#editDetails");
const deleteBtn = viewModal.querySelector("#deleteDetails");
const saveBtn = viewModal.querySelector("#saveDetails");

editBtn.addEventListener("click", () => {
    viewModal.querySelectorAll("input, select").forEach(input => {
        input.disabled = false;
        input.classList.remove("bg-gray-100");
    });
    editBtn.classList.add("hidden");
    deleteBtn.classList.add("hidden");
    saveBtn.classList.remove("hidden");
    document.getElementById("viewName").focus();
});

saveBtn.addEventListener("click", () => {
    const closet = JSON.parse(localStorage.getItem("virtualCloset")) || [];
    const currentId = viewModal.dataset.currentId;
    const index = closet.findIndex(item => item.id == currentId);
    if (index !== -1) {
        closet[index].name = document.getElementById("viewName").value;
        closet[index].type = document.getElementById("viewType").value;
        closet[index].color = document.getElementById("viewColor").value;
        closet[index].gender = document.getElementById("viewGender").value;
        closet[index].season = document.getElementById("viewSeason").value;
        closet[index].usage = document.getElementById("viewUsage").value;
        localStorage.setItem("virtualCloset", JSON.stringify(closet));
        renderCloset();
    }
    viewModal.querySelectorAll("input, select").forEach(input => {
        input.disabled = true;
        input.classList.add("bg-gray-100");
    });
    editBtn.classList.remove("hidden");
    deleteBtn.classList.remove("hidden");
    saveBtn.classList.add("hidden");
    alert("Details updated successfully!");
});

deleteBtn.addEventListener("click", () => {
    const currentId = viewModal.dataset.currentId;
    deleteItem(currentId);
    viewModal.classList.add("hidden");
});

// Render closet items
function renderCloset() {
    const topsContainer = document.querySelector(".tops-container");
    const bottomsContainer = document.querySelector(".bottoms-container");
    topsContainer.innerHTML = "";
    bottomsContainer.innerHTML = "";

    const closet = JSON.parse(localStorage.getItem("virtualCloset")) || [];

    closet.forEach(item => {
        const itemDiv = document.createElement("div");
        itemDiv.className = "relative group";

        const img = document.createElement("img");
        img.src = item.imageSrc;
        img.className = "w-24 h-24 object-cover rounded-md shadow-md cursor-pointer focus:outline focus:ring-2 focus:ring-yellow-400";
        img.tabIndex = 0;
        img.setAttribute("alt", `${item.name}, ${item.color}, ${item.type}`);
        
        document.getElementById("editDetails").focus();

        // On click → show details modal
        img.addEventListener("click", () => {
            document.getElementById("viewModalImage").src = item.imageSrc;
            document.getElementById("viewName").value = item.name;
            document.getElementById("viewType").value = item.type;
            document.getElementById("viewColor").value = item.color;
            document.getElementById("viewGender").value = item.gender;
            document.getElementById("viewSeason").value = item.season;
            document.getElementById("viewUsage").value = item.usage;
            viewModal.dataset.currentId = item.id;

            document.getElementById("editDetails").focus();

            viewModal.querySelectorAll("input, select").forEach(input => {
                input.disabled = true;
                input.classList.add("bg-gray-100");
            });
            editBtn.classList.remove("hidden");
            deleteBtn.classList.remove("hidden");
            saveBtn.classList.add("hidden");
            viewModal.classList.remove("hidden");
        });

        // Also trigger modal on keypress Enter
        img.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                img.click();
                document.getElementById("editDetails").focus();
            }
        });

        itemDiv.appendChild(img);

        if (item.type === "top") {
            topsContainer.appendChild(itemDiv);
        } else {
            bottomsContainer.appendChild(itemDiv);
        }
    });
}




// Delete item
function deleteItem(id) {
    const closet = JSON.parse(localStorage.getItem("virtualCloset")) || [];
    const updatedCloset = closet.filter(item => item.id != id);
    localStorage.setItem("virtualCloset", JSON.stringify(updatedCloset));
    renderCloset();
}


// Render on load
document.addEventListener("DOMContentLoaded", renderCloset);

function resetKeyBuffer() {
    keyBuffer = "";
    if (keyTimer) {
        clearTimeout(keyTimer);
        keyTimer = null;
    }
}

document.addEventListener('keydown', (e) => {

    const tag = document.activeElement.tagName;
    if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') {
        return;
    }

    if (e.key === "Escape") {
        resetKeyBuffer();
        return;
    }

    switch (e.key) {
        case "0":
        window.speechSynthesis.cancel();
        window.location.href = "/homepage";
        return;
        case "1":
        window.speechSynthesis.cancel();
        window.location.href = "/closet";
        return;
        case "2":
        window.speechSynthesis.cancel();
        window.location.href = "/favorites";
        return;
        default:
        break;
    }

    if (e.key === 'X' || e.key === 'x') {
        const logout = document.getElementById('logoutBtn')
        
        if(logout) {
            logout.click();
            speakText("Logout successful!");
        }
        resetKeyBuffer();
        return;
    }
});

function speakText(text) {
    // TTS
    window.speechSynthesis.cancel(); // Addresses simultaneous TTS
    const utterance = new SpeechSynthesisUtterance(text);

    utterance.lang = 'en-US';  
    utterance.volume = 1;
    utterance.rate = 1;
    utterance.pitch = 1;

    window.speechSynthesis.speak(utterance);
}


document.addEventListener("DOMContentLoaded", function() {

    if (window.location.pathname.toLowerCase().endsWith("/closet")) {

        const openCam = document.getElementById("openCamBtn");
        const upload = document.getElementById("uploadBtn");

        openCam.focus();
        speakText("You are currently in the 'Take a Photo' button. Please type enter to take photo");
        
        openCam.addEventListener("focus", function () {
            speakText("You are currently in the 'Take a Photo button'. Please type enter to open camera and take photo");
        });

        upload.addEventListener("focus", function () {
            speakText("You are currently in the 'Upload Image button'. Please type enter to upload photo");
        });

    }
});
