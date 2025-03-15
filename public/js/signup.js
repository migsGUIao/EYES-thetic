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
