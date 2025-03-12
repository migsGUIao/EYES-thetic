// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import { addDoc, collection, query, getDocs } from "firebase/firestore";
import admin from "firebase-admin";
import { getFirestore } from "firebase-admin/firestore";
import serviceAccount from "./eyes-thetic-firebase-adminsdk-fbsvc-4940990a64.json" with { type: "json" };


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

//Firebase admin SDK
admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });

// Initialize Firebase
const app = initializeApp(firebaseConfig);
// const analytics = getAnalytics(app);

// Access Firestore
const firestore = getFirestore();

// Basic CRUD
// Create a document in User collection (register)
export async function createNewUser(userDetails) {
    // Get user details from register page fields
    // Fields: User ID (autogen)[document name], username, display name, email, password
    try {
        const userRef = await firestore.collection("user").add(userDetails);
        console.log("User created with ID:", userRef.id);
    } catch (e) {
        console.error("Error creating user: ", e);
    }
}

// Create a document in Recommedation collection
export async function createNewRecommendation(recommendation, recommDetails) {
    // Get recommendation details from generated recommendations
    // Fields: Recommendation ID (autogen)[document name], recomm-<clothing piece> (list), user ID
}

// Create a document in Review collection
export async function createNewReview(review, reviewDetails) {
    // Get review details from review fields
    // Fields: Review ID (autogen)[document name], number of stars, description, user ID
}

// TODO: Read queries (placeholders currently)
const userQuery = query();
const recommendationQuery = query();
const reviewQuery = query();

// TODO: Get user details 
export async function queryUser() {
    const queryUserSnap = await getDocs(userQuery);
    queryUserSnap.forEach((userSnap) => {
        // get user details through userSnap.data() function
    });
}

// TODO: Get recommendations
export async function queryRecommendation() {
    const queryRecoSnap = await getDocs(recommendationQuery);
    queryRecoSnap.forEach((recoSnap) => {
        // get recommendations through recoSnap.data() function
    });
}

// TODO: Get reviews
export async function queryReview() {
    const queryReviewSnap = await getDocs(reviewQuery);
    queryReviewSnap.forEach((revSnap) => {
        // get reviews through revSnap.data() function
    });
}

// Update document in User collection (username, display name, email, password)
export function updateUser() {
    // Get reference to user document through const user = doc(firestore, userID);
    // Get user details from user fields -> const userDetails = { <fields> };
    // setDoc(user, userDetails, { merge: true });
}

// Delete
export {firestore};