// firebase-config.js
import { initializeApp } from 'https://www.gstatic.com/firebasejs/9.0.0/firebase-app.js';
import { getAuth } from 'https://www.gstatic.com/firebasejs/9.0.0/firebase-auth.js';
import { getFirestore } from 'https://www.gstatic.com/firebasejs/9.0.0/firebase-firestore.js';

// Firebase configuration
const firebaseConfig = {
    apiKey: "AIzaSyBbjcazCQ0dyaV3t4RXNVviBx5P-cX3RPw",
    authDomain: "eyes-thetic.firebaseapp.com",
    projectId: "eyes-thetic",
    storageBucket: "eyes-thetic.appspot.com",
    messagingSenderId: "471553883504",
    appId: "1:471553883504:web:ff838617233ab8611ce3b5",
    measurementId: "G-QN330BE4E4"
};

// Initialize Firebase
const firebaseApp = initializeApp(firebaseConfig);
const auth = getAuth(firebaseApp);
const db = getFirestore(firebaseApp);

export { auth, db };
