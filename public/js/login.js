import { auth, db } from './firebase-config.js';
import { getDocs, collection, query, where } from 'https://www.gstatic.com/firebasejs/9.0.0/firebase-firestore.js';
import { signInWithEmailAndPassword, onAuthStateChanged, signOut } from 'https://www.gstatic.com/firebasejs/9.0.0/firebase-auth.js';

onAuthStateChanged(auth, (user) => {
    const currentPath = window.location.pathname;

    if (user) {
        console.log(`User is logged in: ${user.email}`);

        // If the user is on the login page but already authenticated, redirect to homepage
        if (currentPath === "/login") {
            window.location.href = "/homepage";
        }

    } else {
        console.log("User is logged out.");

        // Redirect to login **ONLY** if the user is on a protected page
        const protectedPages = ["/homepage", "/closet", "/favorites"];
        if (protectedPages.includes(currentPath)) {
            window.location.href = "/login";
        }
    }
});

// Handle Login with Username
document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("loginForm");
    if (loginForm) {
        console.log("Found login form, adding event listener...");
        
        loginForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const username = document.getElementById("username").value.trim();
            const password = document.getElementById("password").value.trim();

            try {
                // Retrieve email from Firestore using username
                const usersRef = collection(db, "user");
                const q = query(usersRef, where("username", "==", username));
                const querySnapshot = await getDocs(q);

                if (querySnapshot.empty) {
                    throw new Error("Username not found. Please check your credentials.");
                }

                // Retrieve email
                let email;
                querySnapshot.forEach((doc) => {
                    email = doc.data().email;
                });

                console.log(`Logging in with email: ${email}`);

                // Sign in with Firebase Auth
                const userCredential = await signInWithEmailAndPassword(auth, email, password);
                const user = userCredential.user;
                const token = await user.getIdToken();

                console.log("Firebase Token:", token);

                // Send token to backend for verification
                const response = await fetch("http://localhost:3000/login", {
                    method: "POST",
                    headers: { 
                        "Content-Type": "application/json",
                        "Authorization": `Bearer ${token}`
                    }
                });

                const result = await response.json();
                console.log("🔹 Server Response:", result);

                if (result.success) {
                    alert("Login successful!");
                    window.location.href = "/homepage";
                } else {
                    alert(result.message);
                }
            } catch (error) {
                console.error("Login error:", error);
                alert(error.message);
            }
        });
    } else {
        console.log("No login form found, skipping login logic.");
    }
});

document.addEventListener("DOMContentLoaded", () => {
    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) {
        console.log("Found logout button, adding event listener...");
        logoutBtn.addEventListener("click", async () => {
            try {
                await signOut(auth); // Logs user out of Firebase
                console.log("User logged out successfully.");

                // Notify the backend (optional)
                await fetch("http://localhost:3000/logout", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" }
                });

                // Redirect to login page
                window.location.href = "/login";
            } catch (error) {
                console.error("Logout error:", error);
                alert("Logout failed. Try again.");
            }
        });
    }
});