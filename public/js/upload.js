/* For the "Upload photo" and "Take a photo" virtual closet feature */

let resnetModel;

// Load the model once the page loads
window.addEventListener("load", async () => {
    try {
        // Update the path below to point to your model.json file
        resnetModel = await tf.loadLayersModel('model.json');
        console.log("ResNet50-based model loaded successfully.");
    } catch (err) {
        console.error("Error loading ResNet50-based model:", err);
    }
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
    const bottomsContainer = document.querySelector(".bottoms-container");    let stream = null; // Store camera stream

    // Open the Camera Popup
    openCameraBtn.addEventListener("click", function () {
        cameraModal.classList.remove("hidden");

        // Start webcam stream
        navigator.mediaDevices.getUserMedia({ video: true })
            .then((cameraStream) => {
                stream = cameraStream;
                video.srcObject = stream;
            })
            .catch((err) => {
                console.error("Error accessing webcam: ", err);
            });
    });

    // Close the Camera Popup
    closeCameraBtn.addEventListener("click", function () {
        cameraModal.classList.add("hidden");

        // Stop the webcam stream
        if (stream) {
            let tracks = stream.getTracks();
            tracks.forEach(track => track.stop());
        }
    });

    // Capture Photo and Display it
    captureBtn.addEventListener("click", function () {
        const context = canvas.getContext("2d");
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        context.drawImage(video, 0, 0, canvas.width, canvas.height);

        // Create an image element
        const imgElement = document.createElement("img");
        imgElement.src = canvas.toDataURL("image/png");
        imgElement.className = "w-24 h-24 object-cover rounded-md shadow-md";
        
        // Append to closet
        detectCategory(imgElement.src)
            .then(category => {
                if (category === "top") {
                    topsContainer.appendChild(imgElement);
                } else {
                    bottomsContainer.appendChild(imgElement);
                }
            })
            .catch(() => {
                topsContainer.appendChild(imgElement);
            });

        // Close the modal after capturing
        cameraModal.classList.add("hidden");
        
        // Stop the webcam stream
        if (stream) {
            let tracks = stream.getTracks();
            tracks.forEach(track => track.stop());
        }
    });
});

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
  
    const prediction = await model.predict(batched).data();
  
    // Assume your model outputs probabilities for 2 classes:
    // Index 0 = "top" and index 1 = "bottom" (adjust as needed).
    const predictedIndex = prediction.indexOf(Math.max(...prediction));
    return predictedIndex === 0 ? "top" : "bottom";
}

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
