const express = require('express');
const bodyParser = require('body-parser');
const csv = require('csv-parser');
const fs = require('fs');
const path = require('path'); // Add this line

const app = express();
const PORT = 3000;

app.use(bodyParser.json());
app.use(express.static('public'));

// Serve the views folder
app.use(express.static(path.join(__dirname, 'views')));

// Default route to serve index.html
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'views', 'index.html'));
});

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
  //console.log(recommendations.slice(0,10));
});

// Start the server
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
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
