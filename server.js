const express = require('express');
const bodyParser = require('body-parser');
const csv = require('csv-parser');
const fs = require('fs');

const app = express();
const PORT = 3000;

// Middleware
app.use(bodyParser.json());
app.use(express.static('public'));

let fashionData = new Map();
let imageData = new Map();

let = missingRecords = 0;
let = totalRecords = 0;

// Load dataset into memory
fs.createReadStream('styles.csv')
  .pipe(csv())
  .on('data', (row) => {

    totalRecords++;

    if (!row.id || !row.gender || !row.season || !row.usage || !row.subcategory || !row.productDisplayName || !row.baseColour) {
      missingRecords++;
      console.log(`⚠️ Skipping row due to missing data:`, row);
      return;
    }
  
    // Push relevant columns only
    fashionData.set(row.id, {
      id: row.id,
      gender: row.gender.toLowerCase().trim(),
      season: row.season.trim(),
      usage: row.usage.trim(),
      subCategory: row.subCategory.trim(),
      productDisplayName: row.productDisplayName.trim(),
      baseColour: row.baseColour.trim(),
    });
  })
  .on('end', () => {
    console.log(`styles.csv loaded into memory with ${fashionData.size} records.`);
    console.log(`📌 Expected records: 44447`);
    console.log(`✅ Successfully loaded: ${fashionData.size}`);
    console.log(`⚠️ Skipped (missing/invalid data): ${missingRecords}`);
    console.log(`🔍 Total rows read from CSV: ${totalRecords}`);
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

  if (!gender || !season || !usage) {
    return res.status(400).json({ error: 'Missing filters: gender, season, or usage.' });
  }

  // Neutral colors
  const neutralColors = new Set(['Black', 'White', 'Grey', 'Beige', 'Navy Blue', 'Brown']);

  // Store matched topwear and bottomwear
  let topwear = [];
  let bottomwear = [];

  // Iterate over Map instead of filtering an array
  for (const item of fashionData.values()) {
    if (
        item.gender.toLowerCase() === gender.toLowerCase() &&
        item.season.toLowerCase() === season.toLowerCase() &&
        item.usage.toLowerCase() === usage.toLowerCase() &&
        neutralColors.has(item.baseColour)
    ) {
        if (item.subCategory === 'Topwear') {
            topwear.push(item);
        } else if (item.subCategory === 'Bottomwear') {
            bottomwear.push(item);
        }
    }
  }

  console.log(`Filtered topwear count: ${topwear.length}`);
  console.log(`Filtered bottomwear count: ${bottomwear.length}`);

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
  res.json(recommendations.slice(0, 10));
});

// Start the server
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});