# The Game of Set

## What is Set?

Set is a real-time card game where players try to find "Sets" among cards laid out on a table. Each card has four attributes, and a valid Set consists of three cards where each attribute is either all the same or all different across the three cards.

## Card Attributes

Each Set card has four attributes:

### 1. Number
- **One**: One symbol on the card
- **Two**: Two symbols on the card
- **Three**: Three symbols on the card

### 2. Shape
- **Diamond**: Diamond-shaped symbols ◆
- **Oval**: Oval-shaped symbols ⬭
- **Squiggle**: Squiggle-shaped symbols ∿

### 3. Color
- **Red**: Red symbols
- **Green**: Green symbols
- **Purple**: Purple symbols

### 4. Shading
- **Solid**: Symbols are completely filled
- **Striped**: Symbols have parallel lines through them
- **Open**: Symbols are just outlines

## What Makes a Valid Set?

For three cards to form a valid Set, **each attribute** must be either:
- **All the same** across the three cards, OR
- **All different** across the three cards

### Examples of Valid Sets

#### Example 1: All Same Except Number
- Card 1: ONE red solid diamond
- Card 2: TWO red solid diamonds
- Card 3: THREE red solid diamonds

✓ Valid because:
- Number: All different (1, 2, 3)
- Shape: All same (diamond)
- Color: All same (red)
- Shading: All same (solid)

#### Example 2: All Different
- Card 1: ONE red solid diamond
- Card 2: TWO green striped ovals
- Card 3: THREE purple open squiggles

✓ Valid because:
- Number: All different (1, 2, 3)
- Shape: All different (diamond, oval, squiggle)
- Color: All different (red, green, purple)
- Shading: All different (solid, striped, open)

#### Example 3: Mixed Same and Different
- Card 1: ONE red solid diamond
- Card 2: ONE green striped diamond
- Card 3: ONE purple open diamond

✓ Valid because:
- Number: All same (1)
- Shape: All same (diamond)
- Color: All different (red, green, purple)
- Shading: All different (solid, striped, open)

### Examples of Invalid Sets

#### Example 1: Two Same, One Different (Number)
- Card 1: ONE red solid diamond
- Card 2: ONE green striped oval
- Card 3: TWO purple open squiggle

✗ Invalid because:
- Number: Two same (1, 1) and one different (2) - violates the rule!

#### Example 2: Two Same, One Different (Color)
- Card 1: TWO red solid diamonds
- Card 2: TWO red striped ovals
- Card 3: TWO green open squiggles

✗ Invalid because:
- Color: Two same (red, red) and one different (green) - violates the rule!

## How This App Helps

This app automatically:
1. **Detects** individual cards in a camera image
2. **Identifies** the four attributes of each card
3. **Finds** all valid Sets among the detected cards
4. **Highlights** the Sets with colored overlays

This is useful for:
- Learning the game
- Checking if you've found all Sets
- Settling disputes about whether a combination is a valid Set
- Practicing Set recognition

## Mathematical Facts

- Total possible cards: 3^4 = **81 cards** in a complete Set deck
- Maximum possible Sets from 12 cards: **14 Sets** (rarely occurs)
- Probability of no Set in 12 random cards: approximately **3.3%**
- Any collection of 21 cards is guaranteed to contain at least one Set

## Tips for Playing Set

1. **Start with one attribute**: Look for three cards with all the same or all different of one attribute first
2. **Check systematically**: Once you find two cards, determine what the third card must be
3. **Practice pattern recognition**: With experience, Sets become easier to spot
4. **Don't overlook the obvious**: Sometimes the simplest Sets are easiest to miss

## References

- [Official Set Game Website](https://www.setgame.com/)
- [Set (card game) on Wikipedia](https://en.wikipedia.org/wiki/Set_(card_game))
