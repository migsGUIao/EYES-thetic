import { auth, db } from './firebase-config.js';
import { createUserWithEmailAndPassword } from 'https://www.gstatic.com/firebasejs/9.0.0/firebase-auth.js';
import { collection, addDoc } from 'https://www.gstatic.com/firebasejs/9.0.0/firebase-firestore.js';

// Handle signup
document.getElementById("signupForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value.trim();
    const username = document.getElementById("username").value.trim();
    const displayName = document.getElementById("displayName").value.trim();

    try {
        const userCredential = await createUserWithEmailAndPassword(auth, email, password);
        const user = userCredential.user;

        await addDoc(collection(db, "user"), {
            uid: user.uid,
            email: email,
            username: username,
            displayName: displayName
        });

        alert("Signup successful! Please log in.");
        window.location.href = "/login";
    } catch (error) {
        console.error("Signup error:", error);
        alert(error.message);
    }
});

document.addEventListener("DOMContentLoaded", function() {

    if (window.location.pathname.toLowerCase().endsWith("/signup")) {
        const displayName = document.getElementById("displayName");
        const usernameField = document.getElementById("username");
        const email = document.getElementById("email");
        const passwordField = document.getElementById("password");
        const signup = document.getElementById("signup");

        displayName.focus();
        speakText("You are currently in the sign up page. Please enter your display name. Please Press TAB to enter username.");
        
        displayName.addEventListener("focus", function () {
            speakText("You are currently in the sign up page. Please enter your display name. Please Press TAB to enter username.");
        });

        usernameField.addEventListener("focus", function () {
            speakText("Please enter your user name. Please Press TAB to enter email.");
        });

        email.addEventListener("focus", function () {
            speakText("Please enter your email. Please Press TAB to enter password.");
        });

        passwordField.addEventListener("focus", function () {
            speakText("This is the last field! Please enter your password. Press on TAB to click enter and finally Sign up!");
        });

        signup.addEventListener("focus", function () {
            speakText("Click Sign up!");
        });
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
