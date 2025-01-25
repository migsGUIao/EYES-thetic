const express = require('express');
const bodyParser = require('body-parser');
const csv = require('csv-parser');
const fs = require('fs');

const app = express();
const PORT = 3000;

// Middleware
app.use(bodyParser.json());
app.use(express.static('public'));

// Load dataset into memory
let fashionData = [];
fs.createReadStream('styles.csv')
  .pipe(csv())
  .on('data', (row) => {
    // Push relevant columns only
    fashionData.push({
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
    console.log('CSV file successfully processed.');
  });

// Recommendation route
app.post('/recommend', (req, res) => {
  const { gender, season, usage } = req.body;

  if (!gender || !season || !usage) {
    return res.status(400).json({ error: 'Missing filters: gender, season, or usage.' });
  }

  // Define neutral colors
  const neutralColors = ['Black', 'White', 'Grey', 'Beige', 'Navy Blue', 'Brown'];

  // Filter data
  const filteredData = fashionData.filter((item) => {
    return (
      item.gender.toLowerCase() === gender.toLowerCase() &&
      item.season.toLowerCase() === season.toLowerCase() &&
      item.usage.toLowerCase() === usage.toLowerCase() &&
      neutralColors.includes(item.baseColour)
    );
  });

  console.log('Filtered Data:', filteredData); // Log the filtered data

  // Separate topwear and bottomwear
  const topwear = filteredData.filter(item => item.subCategory === 'Topwear');
  const bottomwear = filteredData.filter(item => item.subCategory === 'Bottomwear');

  console.log('Topwear:', topwear); // Log topwear items
  console.log('Bottomwear:', bottomwear); // Log bottomwear items

  // Pair tops and bottoms
  const recommendations = [];
  for (const top of topwear) {
      for (const bottom of bottomwear) {
        recommendations.push({
            top_id: top.id,
            top_name: top.productDisplayName,
            top_colour: top.baseColour,
            bottom_id: bottom.id,
            bottom_name: bottom.productDisplayName,
            bottom_colour: bottom.baseColour,
            season: top.season, // Both should match in filteredData
            usage: top.usage   // Both should match in filteredData
        });
      }
  }

  console.log('Recommendations:', recommendations); // Log final recommendations


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