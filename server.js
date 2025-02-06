const express = require('express');
const bodyParser = require('body-parser');
const csv = require('csv-parser');
const fs = require('fs');

const app = express();
const PORT = 3000;

app.use(bodyParser.json());
app.use(express.static('public'));

let fashionData = new Map();
let imageData = new Map();

// Load dataset into memory
fs.createReadStream('styles.csv')
  .pipe(csv())
  .on('data', (row) => {
  
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
  console.log(`Received request: Gender=${gender}, Season=${season}, Usage=${usage}`);


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
        if (item.subCategory === 'Topwear' && item.gender === 'Men' || item.subCategory === 'Topwear' && item.gender === 'Women') {
            topwear.push(item);
        } else if (item.subCategory === 'Bottomwear' && item.gender === 'Men' || item.subCategory === 'Bottomwear' && item.gender == 'Women') {
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
  res.json(recommendations.slice(0, 10));
  console.log(recommendations.slice(0,10));
});

// Start the server
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});