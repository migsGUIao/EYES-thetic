let recommendations = [];
let currentIndex = 0;
window.currentRecommendation = null;

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
        let recs = result;

        recs = reRankRecommendations(recs);
        recommendations = recs;
        currentIndex = 0;

        // Display the first recommendation
        
        showNextRecommendation();
    } catch (error) {
        console.error('Error fetching recommendations:', error);
        document.getElementById('recommendation').innerHTML = `<p>Error fetching recommendations. Please try again later.</p>`;
    }
}

//Random Recommendation
document.getElementById('randomRecommendationBtn').addEventListener('click', showRandomRecommendation);

async function showRandomRecommendation() {
    try {
        const response = await fetch('/random-recommend');
        const recommendation = await response.json();

        if (recommendation.message) {
            document.getElementById('recommendation').innerHTML = `<p>${recommendation.message}</p>`;
            window.currentRecommendation = null;
            return;
        }

        const rec = recommendation;
        window.currentRecommendation = rec;

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

            </div>
        `;
        document.getElementById('recommendation').innerHTML = html;

        const choices = `You have picked: Gender ${rec.gender} Season: ${rec.season} Usage: ${rec.usage}`;
        const description = `Randomly recommended outfit. Top: ${rec.top_name} Color: ${rec.top_colour}. Bottom: ${rec.bottom_name} Color: ${rec.bottom_colour}.`;
        const result = choices.concat(description)
        speakText(result);

        document.getElementById('nextRecommendation').style.display = 'none';
        document.getElementById('favoriteBtn').style.display = 'block';
        document.getElementById('favoriteBtn').onclick = function () {
            saveFavorite(rec);
        };

    } catch (error) {
        console.error('Error fetching random recommendation:', error);
        document.getElementById('recommendation').innerHTML = `<p>Error fetching random recommendation. Please try again later.</p>`;
    }
}

function showNextRecommendation() {
    resetKeyBuffer();

    // Interrupt ongoing speech to make way for new ones
    window.speechSynthesis.cancel()

    if (currentIndex >= recommendations.length) {
        document.getElementById('recommendation').innerHTML = "<p>No more recommendations available.</p>";
        document.getElementById('nextRecommendation').style.display = 'none';
        document.getElementById('favoriteBtn').style.display = 'none';

        window.currentRecommendation = null;
        return;
    }

    const rec = recommendations[currentIndex];
    window.currentRecommendation = rec;

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
    const choices = `You have picked: Gender ${rec.gender} Season: ${rec.season} Usage: ${rec.usage}`;
    const description = `Recommended outfit. Top: ${rec.top_name} Color: ${rec.top_colour}. Bottom: ${rec.bottom_name} Color: ${rec.bottom_colour}.`;
    const result = choices.concat(description)
    speakText(result);

    document.getElementById('nextRecommendation').style.display =
        (currentIndex < recommendations.length - 1) ? 'block' : 'none';

    // Show favorite button and update its function dynamically
    const favoriteBtn = document.getElementById('favoriteBtn');
    favoriteBtn.style.display = 'block';
    favoriteBtn.onclick = function () {
        saveFavorite(rec);
    };

    currentIndex++;
}

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

// TTS For WELCOME homepage
document.addEventListener("DOMContentLoaded", function() {
    if (window.location.pathname.toLowerCase().endsWith("/homepage")) {
        const welcome = document.getElementById("welcome").innerText;
        const intro = document.getElementById("intro").innerText;
        const random = "Try our Random Recommendation feature, press R on your keyboard!";
        const concat = welcome.concat(intro)
        const final = concat.concat(random)
        speakText(final);
    }
});

function saveFavorite(rec) {
    // Retrieve existing favorites from localStorage - TEMPORARY
    let favorites = JSON.parse(localStorage.getItem('favorites')) || [];
    
    // Check if the recommendation already exists to avoid duplicates
    if (!favorites.some(item => item.top_id === rec.top_id && item.bottom_id === rec.bottom_id)) {
        rec.timestamp = new Date().getTime();
        favorites.push(rec);
        localStorage.setItem('favorites', JSON.stringify(favorites));
        alert('Favorite saved!');
        speakText('Favorite saved!');
    } else {
        alert('This recommendation is already in favorites!');
        speakText('This recommendation is already in favorites!')
    }
}

function reRankRecommendations(recommendations) {
    // Retrieve favorites from localStorage (assumed to be saved as an array of recommendation objects)
    let favorites = JSON.parse(localStorage.getItem('favorites')) || [];

    // Build frequency maps for top and bottom colors from favorites
    let topColorFreq = {};
    let bottomColorFreq = {};

    favorites.forEach(fav => {
        if (fav.top_colour) {
            topColorFreq[fav.top_colour] = (topColorFreq[fav.top_colour] || 0) + 1;
        }
        if (fav.bottom_colour) {
            bottomColorFreq[fav.bottom_colour] = (bottomColorFreq[fav.bottom_colour] || 0) + 1;
        }
    });

    // Compute a similarity score for each recommendation:
    // score = (frequency of top_colour in favorites) + (frequency of bottom_colour in favorites)
    recommendations.forEach(rec => {
        let score = 0;
        if (rec.top_colour && topColorFreq[rec.top_colour]) {
            score += topColorFreq[rec.top_colour];
        }
        if (rec.bottom_colour && bottomColorFreq[rec.bottom_colour]) {
            score += bottomColorFreq[rec.bottom_colour];
        }
        rec.similarityScore = score;
    });

    // Sort recommendations by similarityScore (highest first)
    recommendations.sort((a, b) => b.similarityScore - a.similarityScore);

    return recommendations;
}

  
// Keybinds
let keyTimer = null;

const allowedKeys = new Set(['M', 'L', 'F', 'Z', 'W', 'Q', 'C', 'S']);

function updateVisualFeedback() {
    // For example, update an element with id "keyBufferDisplay"
    const display = document.getElementById('keyBufferDisplay');
    if (display) display.textContent = keyBuffer;
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

    if (e.key.startsWith("Arrow")) {
        if (e.key === "ArrowRight") showNextRecommendation();
        return;
    }

    if (e.key === "Escape") {
        resetKeyBuffer();
        return;
    }

    // "R" key triggers the random recommendation button (only on homepage)
    if (e.key === 'r' || e.key === 'R') {
        const randomBtn = document.getElementById("randomRecommendationBtn");
        if (randomBtn) {
            randomBtn.click();
        }
        resetKeyBuffer();
        return;
    }

     // Check the pressed key.
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

    if (e.key === 'a' || e.key === 'A') {
        if (window.currentRecommendation) {
            saveFavorite(window.currentRecommendation);
            showNextRecommendation();
        } else {
            console.warn("No current recommendation available.");
        }
        resetKeyBuffer();
        return;
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

    // Only allow keys that are in the allowed set.
    let pressedKey = e.key.toUpperCase();
    if (!allowedKeys.has(pressedKey)) {
        console.warn(`Ignored key: ${pressedKey}. Resetting key buffer.`);
        resetKeyBuffer();
        return;
    }

    
    keyBuffer += pressedKey;
    updateVisualFeedback();
    console.log("Current key buffer:", keyBuffer);

    // If the buffer is not yet complete, set a timeout for 1 sec to reset.
    if (keyTimer) clearTimeout(keyTimer);
    keyTimer = setTimeout(() => {
        console.log("Key buffer timed out, resetting.");
        resetKeyBuffer();
    }, 1500);

    // When we have exactly 3 characters, attempt to map them.
    if (keyBuffer.length === 3) {
        console.log("Full key sequence:", keyBuffer);
        const code = keyBuffer;
        resetKeyBuffer(); 
        
        const genderMapping = { 'M': 'Men', 'L': 'Women' };
        const seasonMapping = { 'F': 'Fall', 'Z': 'Summer', 'W': 'Winter', 'Q': 'Spring' };
        const usageMapping  = { 'C': 'Casual', 'F': 'Formal', 'S': 'Sports' };

        const genderVal = genderMapping[code.charAt(0)];
        const seasonVal = seasonMapping[code.charAt(1)];
        const usageVal  = usageMapping[code.charAt(2)];

        console.log("Mapped values:", genderVal, seasonVal, usageVal);

        if (genderVal && seasonVal && usageVal) {
            document.getElementById('gender').value = genderVal;
            document.getElementById('season').value = seasonVal;
            document.getElementById('usage').value = usageVal;

            submitForm();
        }
    }

});