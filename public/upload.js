/* For the "Upload photo" and "Take a photo" virtual closet feature */

let model; // Global variable for the model

// Load the model on page load. Adjust the path to your model.json.
tf.loadLayersModel('/path/to/your/model.json').then(m => {
  model = m;
  console.log("Model loaded successfully.");
}).catch(err => {
  console.error("Failed to load model", err);
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
    if (!model) {
      throw new Error("Model is not loaded yet!");
    }
  
    // Create an image element and wait for it to load.
    const img = new Image();
    img.crossOrigin = "anonymous"; // in case of CORS issues
    img.src = imgSrc;
  
    await new Promise((resolve, reject) => {
      img.onload = resolve;
      img.onerror = reject;
    });
  
    // Convert the image to a tensor and preprocess it.
    let tensor = tf.browser.fromPixels(img)
      .resizeNearestNeighbor([224, 224]) // resize to model's input size
      .toFloat()
      .div(tf.scalar(255)); // normalize pixel values between 0 and 1
  
    // Expand dims to create a batch of 1.
    const batched = tensor.expandDims(0);
  
    // Run inference.
    const prediction = await model.predict(batched).data();
  
    // Assume your model outputs probabilities for 2 classes:
    // Index 0 = "top" and index 1 = "bottom" (adjust as needed).
    const predictedIndex = prediction.indexOf(Math.max(...prediction));
    return predictedIndex === 0 ? "top" : "bottom";
  }