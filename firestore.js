// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import { getFirestore } from "firebase/firestore";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
    apiKey: "AIzaSyBbjcazCQ0dyaV3t4RXNVviBx5P-cX3RPw",
    authDomain: "eyes-thetic.firebaseapp.com",
    projectId: "eyes-thetic",
    storageBucket: "eyes-thetic.firebasestorage.app",
    messagingSenderId: "471553883504",
    appId: "1:471553883504:web:ff838617233ab8611ce3b5",
    measurementId: "G-QN330BE4E4"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);

// Access Firestore
const firestore = getFirestore();

// Basic CRUD