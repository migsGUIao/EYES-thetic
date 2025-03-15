import express from "express";
import bodyParser from "body-parser";
import csv from "csv-parser";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";  
import { firestore, verifyFirebaseToken, createNewUser } from "./firestore.js";  
import { hash, verify } from "argon2";

// import { createNewUser, createNewRecommendation, createNewReview, queryUser,
//          queryRecommendation, queryReview, updateUser
//        } from './firestore.js'; // CRUD operations to firestore db

const app = express();
const PORT = 3000;

app.use(bodyParser.json());
app.use(express.static('public'));

// Fix __dirname for ES Modules
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Serve the views folder
app.use(express.static(path.join(__dirname, 'views')));

// Default route (localhost:3000) should load login.html
app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "views", "login.html"));
});

app.get("/login", (req, res) => {
  res.sendFile(path.join(__dirname, "views", "login.html"));
});

app.get("/signup", (req, res) => {
  res.sendFile(path.join(__dirname, "views", "signup.html"));
});

app.get("/homepage", (req, res) => {
  res.sendFile(path.join(__dirname, "views", "index.html"));
});

app.get("/favorites", (req, res) => {
  res.sendFile(path.join(__dirname, "views", "favorites.html"));
});

app.get("/closet", (req, res) => {
  res.sendFile(path.join(__dirname, "views", "closet.html"));
});

let fashionData = new Map();
let imageData = new Map();

// Load dataset into memory
fs.createReadStream('styles.csv')
  .pipe(csv())
  .on('data', (row) => {
    
    if (
      row.productDisplayName.toLowerCase().includes("kids") &&
      row.gender.trim().toLowerCase() === "men"
    ) {
      row.gender = "Boys";
    }
  
    // Push relevant columns only
    fashionData.set(row.id, {
      id: row.id,
      gender: row.gender,
      season: row.season,
      usage: row.usage,
      subCategory: row.subCategory,
      productDisplayName: row.productDisplayName,
      baseColour: row.baseColour,
    });
  })
  .on('end', () => {
    console.log(`styles.csv loaded into memory with ${fashionData.size} records.`);
  });

// load images
fs.createReadStream('images.csv')
    .pipe(csv())
    .on('data', (row) => {
        imageData.set(row.filename.replace('.jpg', ''), row.link); // Remove .jpg extension for consistency with styles.csv
    })
    .on('end', () => {
        console.log(`images.csv loaded into memory with ${imageData.size} records.`);
   });

// Recommendation route
app.post('/recommend', (req, res) => {
  const { gender, season, usage } = req.body;
  console.log(`\nReceived request: Gender=${gender}, Season=${season}, Usage=${usage}`);

  if (!gender || !season || !usage) {
    return res.status(400).json({ error: 'Missing filters: gender, season, or usage.' });
  }

  const neutralColors = new Set(['Black', 'White', 'Grey', 'Beige', 'Navy Blue', 'Brown']);
  
  const seasonColorMen = {
    fall: new Set(['Maroon', 'Burgundy', 'Coffee Brown', 'Mushroom Brown', 'Rust', 'Olive', 'Mustard', 'Taupe']),
    summer: new Set(['Blue', 'Teal', 'Turquoise Blue', 'Fluorescent Green', 'Magenta', 'Lime Green', 'Sea Green', 'Lavender']),
    winter: new Set(['Navy Blue', 'Blue', 'Teal', 'Turquoise Blue', 'Fluorescent Green', 'Magenta']),
    spring: new Set(['Off White', 'Cream', 'Beige', 'Tan', 'Taupe', 'Nude', 'Peach', 'Yellow', 'Pink', 'Khaki', 'Skin'])
  };
  const menColors = seasonColorMen[season.toLowerCase()];

  const seasonColorWomen = {
    fall: new Set(['Brown', 'Bronze', 'Copper', 'Maroon', 'Coffee Brown', 'Olive', 'Burgundy', 'Rust', 'Mustard', 'Taupe, Mushroom Brown']),
    summer: new Set(['Silver', 'Grey', 'Grey Melange', 'Steel', 'Lavender', 'Sea Green', 'Mauve', 'Rose']),
    winter: new Set(['Blue', 'Turquoise Blue', 'Teal', 'Magenta']),
    spring: new Set(['Off White', 'Cream', 'Peach', 'Beige', 'Tan', 'Taupe', 'Nude' , 'Yellow', 'Skin'])
  };
  const womenColors = seasonColorWomen[season.toLowerCase()];

  // Store matched topwear and bottomwear
  let topwear = [];
  let bottomwear = [];

  let topColors = []
  let bottomColors = []

  // Iterate over Map instead of filtering an array
  for (const item of fashionData.values()) {
      topColors.push(item.baseColour)
      bottomColors.push(item.baseColour)

      if (item.gender === gender && item.season === season && item.usage === usage) {

          if (gender === 'Men' && menColors.has(item.baseColour)) {
              if (item.subCategory === 'Topwear') {
                  topwear.push(item);
              } else if (item.subCategory === 'Bottomwear') {
                  bottomwear.push(item);
              }
          }

          else if (gender === 'Women' /*&& womenColors.has(item.baseColour)*/) {
              if (item.subCategory === 'Topwear') {
                  topwear.push(item);
              } else if (item.subCategory === 'Bottomwear') {
                  bottomwear.push(item);
              }
          }
      }
  }

  // Topwear and bottomwear count!
  console.log(`\nFiltered topwear count: ${topwear.length}`);
  console.log(`Filtered bottomwear count: ${bottomwear.length}`);

  console.log(`\nTopwear colors: ${removeDuplicates(topColors)}`);
  console.log(`Bottomwear colors: ${removeDuplicates(bottomColors)}`);

  // Pair tops and bottoms
  const recommendations = [];
  for (const top of topwear) {
      for (const bottom of bottomwear) {

        recommendations.push({
            top_id: top.id,
            top_name: top.productDisplayName,
            top_colour: top.baseColour,
            top_image: imageData.get(top.id) || null, // O(1) lookup
            bottom_id: bottom.id,
            bottom_name: bottom.productDisplayName,
            bottom_colour: bottom.baseColour,
            bottom_image: imageData.get(bottom.id) || null, // O(1) lookup
            gender: gender,
            season: top.season, // Both should match in filteredData
            usage: top.usage   // Both should match in filteredData

        });
      }
  }

  console.log(`Generated ${recommendations.length}`); // Log final recommendations

  if (recommendations.length === 0) {
    return res.json({ message: 'No matching recommendations found.' });
  }

  // Return a limited set of recommendations (e.g., top 10)
  res.json(recommendations.slice(0, 50));
  console.log(recommendations.slice(0,10));
});



// Sanity check function to remove duplicate colors
function removeDuplicates(data) {
  let unique = []
  data.forEach(element => {
    if (!unique.includes(element)) {
      unique.push(element)
    }
  });
  return unique;
}


// Firestore CRUD below

// TODO: Document templates
// User Fields:
//    User ID (autogen)[document name],
//    username,
//    display name,
//    email,
//    password

// Recommendation Fields:
//    Recommendation ID (autogen)[document name],
//    recomm-<clothing piece> (list),
//    user ID

// Review Fields:
//    Review ID (autogen)[document name],
//    number of stars,
//    description,
//    user ID

//signup
app.post("/signup", async (req, res) => {
    const { username, displayName, email, password } = req.body;

    if (!username || !email || !password) {
        return res.status(400).json({ success: false, message: "Missing required fields." });
    }

    const hashPw = await hash(password);

    try {
        // Call function from firestore.js to add the user
        await createNewUser({ username, displayName, email, password: hashPw });

        res.json({ success: true, message: "User registered successfully!" });
    } catch (error) {
        console.error("Signup error:", error);
        res.status(500).json({ success: false, message: "Signup failed!" });
    }
});

//login
app.post("/login", verifyFirebaseToken, (req, res) => {
  res.json({ success: true, message: "Login successful!", user: req.user });
 /*  const { username, password } = req.body;

  if (!username || !password) {
      return res.status(400).json({ success: false, message: "Username and password are required!" });
  }

  try {
      const usersRef = firestore.collection("user");
      const querySnapshot = await usersRef.where("username", "==", username).get();

      if (querySnapshot.empty) {
          return res.status(401).json({ success: false, message: "Invalid username or password" });
      }

      // Retrieve user data
      let userData = null;
      querySnapshot.forEach(doc => {
          userData = doc.data();
      });

      // Compare passwords TO DO: HASHING
      const pwComp = await verify(userData.password, password); // true: password match

      // if (userData.password !== password) {
      if (!pwComp) {
          return res.status(401).json({ success: false, message: "Invalid username or password" });
      }

      // Login successful
      res.json({ success: true, message: "Login successful!" });

  } catch (error) {
      console.error("Login error:", error);
      res.status(500).json({ success: false, message: "Server error. Please try again later." });
  } */
});

app.post("/logout", (req, res) => {
  try {
      // Firebase authentication is client-side, so we just clear session storage
      res.json({ success: true, message: "User logged out successfully." });
  } catch (error) {
      console.error("Logout error:", error);
      res.status(500).json({ success: false, message: "Logout failed." });
  }
});

// Start the server
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
