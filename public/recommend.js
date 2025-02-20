let recommendations = [];
let currentIndex = 0;

async function submitForm() {
    const form = document.getElementById('recommendForm');
    
    console.log("recommend.js is loaded!"); // Debugging

    // collect form data
    const data = {
        gender: form.gender.value,
        season: form.season.value,
        usage: form.usage.value
    };

    try {
        // Send data to the backend
        const response = await fetch('/recommend', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        const result = await response.json();

        if (result.message) {
            document.getElementById('recommendation').innerHTML = `<p>${result.message}</p>`;
            return;
        }

        // Save the recommendations and reset the current index
        recommendations = result;
        currentIndex = 0;

        // Display the first recommendation
        showNextRecommendation();
    } catch (error) {
        console.error('Error fetching recommendations:', error);
        document.getElementById('recommendation').innerHTML = `<p>Error fetching recommendations. Please try again later.</p>`;
    }
}

function showNextRecommendation() {
    // Interrupt ongoing speech to make way for new ones
    window.speechSynthesis.cancel();

    if (currentIndex >= recommendations.length) {
        document.getElementById('recommendation').innerHTML = "<p>No more recommendations available.</p>";
        document.getElementById('nextRecommendation').style.display = 'none';
        return;
    }

    const rec = recommendations[currentIndex];

    const html = `
        <div class="recommendation-box">
            <h4>Topwear</h4>
            ${rec.top_image ? `<img src="${rec.top_image}" alt="${rec.top_name}" />` : '<p>No Image Available</p>'}
            <p><b>Top:</b> ${rec.top_name} (${rec.top_colour})</p>

            <h4>Bottomwear</h4>
            ${rec.bottom_image ? `<img src="${rec.bottom_image}" alt="${rec.bottom_name}" />` : '<p>No Image Available</p>'}
            <p><b>Bottom:</b> ${rec.bottom_name} (${rec.bottom_colour})</p>
            
            <p><b>Gender:<b> ${rec.gender}</p>
            <p><b>Season:</b> ${rec.season}</p>
            <p><b>Usage:</b> ${rec.usage}</p>

            <!-- Favorite Button -->
            <button onclick='saveFavorite(${JSON.stringify(rec)})'>Favorite</button>

        </div>
    `;
    document.getElementById('recommendation').innerHTML = html;

    // Use the SpeechSynthesis API for TTS
    const description = `Recommended outfit. Top: ${rec.top_name}. Bottom: ${rec.bottom_name}.`;
    speakText(description);

    document.getElementById('nextRecommendation').style.display =
        (currentIndex < recommendations.length - 1) ? 'block' : 'none';

    currentIndex++;
}

function speakText(text) {
    // TTS
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'en-US';  

    utterance.volume = 1;
    utterance.rate = 1;
    utterance.pitch = 1;

    window.speechSynthesis.speak(utterance);
}

function saveFavorite(rec) {
    // Retrieve existing favorites from localStorage
    let favorites = JSON.parse(localStorage.getItem('favorites')) || [];
    
    // Optionally, check if the recommendation already exists to avoid duplicates
    if (!favorites.some(item => item.top_id === rec.top_id && item.bottom_id === rec.bottom_id)) {
        favorites.push(rec);
        localStorage.setItem('favorites', JSON.stringify(favorites));
        alert('Favorite saved!');
    } else {
        alert('This recommendation is already in favorites!');
        }
  }

// Keybinds
let keyBuffer = "";
let keyTimer = null;

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

    if (e.key === "ArrowRight") {
        showNextRecommendation();
    }

    keyBuffer += e.key.toUpperCase();

    // If the buffer is not yet complete, set a timeout for 1 sec to reset.
    if (keyBuffer.length < 3) {
        if (keyTimer) {
            clearTimeout(keyTimer);
        }
        keyTimer = setTimeout(resetKeyBuffer, 1000);
    }

    // When we have exactly 3 characters, attempt to map them.
    if (keyBuffer.length === 3) {
        const code = keyBuffer;
        resetKeyBuffer(); 
        
        const genderMapping = { 'M': 'Men', 'L': 'Women' };
        const seasonMapping = { 'F': 'Fall', 'Z': 'Summer', 'W': 'Winter', 'Q': 'Spring' };
        const usageMapping  = { 'C': 'Casual', 'F': 'Formal', 'S': 'Sports' };

        const genderVal = genderMapping[code.charAt(0)];
        const seasonVal = seasonMapping[code.charAt(1)];
        const usageVal  = usageMapping[code.charAt(2)];

        if (genderVal && seasonVal && usageVal) {
            document.getElementById('gender').value = genderVal;
            document.getElementById('season').value = seasonVal;
            document.getElementById('usage').value = usageVal;

            submitForm();
        }
    }
});