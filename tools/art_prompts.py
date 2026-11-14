"""
Writes the prompts for the pictures drawn with ChatGPT image generation through Codex: the pets (four stages in a
row), their film cans (the eggs), the booth with its things and the hall. Run tools/art_gen.py to draw them and
tools/cut.py to cut them into the app.

    python tools/art_prompts.py
"""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ART = os.path.join(ROOT, "art")

# id, what it is, stage by stage, colours. The order is the app's Species order.
PETS = [
    ("cat", "A cute round grey-blue cat, a noir detective cinema usher with a soft white muzzle and belly.",
     "Baby: a tiny kitten with a little torch-light in its paws. Young: a small grey fedora. Adult: a trench-coat collar and the fedora tipped low, a cinema ticket tucked in the hat band. Ancient: a long trench coat, silver whiskers, a pocket watch chain and a tiny magnifying glass.",
     "Slate blue-grey fur, white muzzle, pink inner ears, charcoal fedora with a red band."),
    ("fox", "A cute fluffy orange fox, a cartoon animator with a big white-tipped tail.",
     "Baby: a tiny fox kit holding a pencil stub. Young: white cartoon gloves. Adult: a striped scarf and a little film strip coiled round its tail. Ancient: a flowing tail with film-frame pattern, a beret and a golden pencil behind its ear.",
     "Bright orange fur, cream belly and tail tip, dark brown paws, red scarf."),
    ("owl", "A round fluffy owl, a film critic with large ivory face discs.",
     "Baby: a grey-brown owlet with huge eyes. Young: tiny round reading glasses. Adult: a striped critic's scarf and a little notebook under its wing. Ancient: bushy white eyebrows, gold-rimmed glasses and a tiny golden statuette pin on its chest.",
     "Warm brown feathers, ivory face and belly with speckles, orange beak and feet."),
    ("bat", "A round fluffy purple bat who loves horror films, more cute than scary.",
     "Baby: a tiny purple fuzzball with big ears and little fangs. Young: wider wings with a moon-shaped spot. Adult: a high Dracula collar cape in red and black. Ancient: a long cape with gold clasp, silver fur tips and a tiny candle-lantern.",
     "Dusky purple fur, lilac belly, pink inner ears, red cape lining."),
    ("coon", "A cute grey raccoon, a comedian and popcorn thief.",
     "Baby: a tiny raccoon holding one popcorn kernel (its eyes clearly visible inside the black mask). Young: a red clown nose. Adult: a striped popcorn bucket hugged in its arms and a little bow tie. Ancient: a big overflowing popcorn bucket, a top hat with a flower, a happy grin.",
     "Grey fur, black mask and tail rings, red and white stripes, golden popcorn."),
    ("hamster", "A cute round golden hamster astronaut who loves science fiction.",
     "Baby: a tiny hamster with puffy cheeks and a little antenna headband. Young: a round glass space helmet (its face clearly visible through it). Adult: a white space suit with an orange stripe and a ray-gun shaped popcorn scoop. Ancient: a silver suit with glowing blue lights, a jetpack and a star badge.",
     "Golden-orange fur, cream cheeks, white and silver suit, cyan glow."),
    ("armadillo", "A cute chubby armadillo cowboy who loves westerns.",
     "Baby: a tiny tan armadillo with soft banded shell. Young: a small brown cowboy hat. Adult: a red bandana and a sheriff star on its chest. Ancient: a poncho, spurs, a long cowboy hat and a cactus flower on the hat.",
     "Sandy tan shell bands, pink-beige face, brown leather hat, red bandana, gold star."),
    ("lovebird", "A cute round lovebird who loves romance films.",
     "Baby: a tiny pastel green chick with rosy cheeks. Young: a heart-shaped crest feather. Adult: a pink rose tucked behind its head and heart patterns on its wings. Ancient: a long ribbon-like tail, a small crown of roses and floating tiny hearts in its feathers.",
     "Mint green body, peach-pink face, rose red accents."),
    ("monkey", "A cute small brown monkey, an adventurer explorer.",
     "Baby: a tiny monkey holding a banana-coloured map scroll. Young: a pith helmet. Adult: an explorer's vest with pockets and a coiled rope on its shoulder. Ancient: a worn leather hat, a brass compass medallion, and a bushy grey beard.",
     "Warm brown fur, beige face and belly, khaki and leather browns."),
    ("frog", "A cute green frog, a musical star tap dancer.",
     "Baby: a tiny green froglet with a shiny bow tie dot. Young: a little black top hat. Adult: a top hat, a red bow tie and a cane held up. Ancient: a sequined golden waistcoat, a top hat with a feather and white spats.",
     "Bright green skin, yellow belly, black hat, red and gold."),
    ("moth", "A cute fuzzy moth drawn to the projector's light; it watches films late at night.",
     "Baby: a tiny fluffy cream moth with feathery antennae. Young: small wings with a film-reel eye pattern. Adult: big soft wings with a moon and stars pattern. Ancient: wings shining like a projector beam, glowing golden dust around their edges.",
     "Cream fluff, night blue and lavender wings, warm golden glow."),
    ("sloth", "A cute sleepy sloth who binge-watches series in a blanket.",
     "Baby: a tiny sloth curled round a pillow. Young: a cosy striped blanket over its shoulders. Adult: the blanket, fluffy slippers and a TV remote. Ancient: a huge patchwork blanket, a nightcap and a mug of cocoa.",
     "Soft brown-grey fur, cream face mask, dark eye patches (eyes still big and visible), red-and-cream blanket."),
    ("penguin", "A cute round penguin from silent-film days who loves old classic films.",
     "Baby: a tiny grey fluffy penguin chick. Young: a little black bowler hat. Adult: the bowler hat, a small moustache and a bamboo cane. Ancient: a sepia-toned tuxedo look, a monocle and a silver film-reel medal.",
     "Black and white, grey chick fluff, cream tones like old film."),
    ("redpanda", "A cute red panda traveller who watches films from every country.",
     "Baby: a tiny red panda with a striped tail. Young: a little knitted scarf. Adult: a backpack with travel stickers and a passport poking out. Ancient: a long patchwork travel cloak, a straw hat and a tiny globe.",
     "Rusty red fur, white face marks, dark legs, colourful travel stickers."),
    ("pug", "A cute chubby pug who loves watching films with friends.",
     "Baby: a tiny fawn pug puppy with a curly tail. Young: a party popcorn box on its head like a hat. Adult: 3D cinema glasses with red and blue lenses pushed up on its forehead and a friendship bracelet. Ancient: a cosy cardigan, a big shared popcorn tub and a small crown.",
     "Fawn fur, black face, red and blue glasses, striped popcorn box."),
    ("mouse", "A cute little mouse who always stays after the credits.",
     "Baby: a tiny grey mouse with big round ears holding a ticket stub. Young: a usher's flashlight. Adult: a red usher's cap and jacket with golden buttons. Ancient: a long usher's coat, a ring of brass keys and a little golden star badge.",
     "Soft grey fur, pink ears and tail, red and gold uniform."),
    ("goldfish", "A cute round goldfish floating in the air in a bubble, who loves rewatching films (it forgets the ending every time).",
     "Baby: a tiny orange fish in a small bubble. Young: flowing fins and a little bubble around it. Adult: long veil-tail fins with a film-strip pattern. Ancient: shimmering golden veil fins, pearls and sparkles.",
     "Orange and gold scales, white fin tips, pale aqua bubble."),
    ("squirrel", "A cute squirrel who hoards series like nuts.",
     "Baby: a tiny squirrel hugging an acorn. Young: an acorn-cap hat. Adult: a stack of tiny film cans held like nuts and a big fluffy tail. Ancient: a magnificent tail, a striped scarf, glasses, and a tiny golden acorn on a chain.",
     "Warm chestnut fur, cream belly, brown acorns, silver film cans."),
    # Creatures of fairy tales and films, not only real animals.
    ("ghost", "A cute little cinema ghost: a soft round white sheet-ghost who haunts the back row at midnight films.",
     "Baby: a tiny wisp of a ghost with a wavy tail. Young: a ticket stub stuck to it like a badge. Adult: a tattered usher's cap and a little lantern with a blue flame. Ancient: a long flowing translucent cloak with film-strip edging, a ring of floating blue candle flames.",
     "Pearl white with a lavender and pale cyan glow, translucent look made with pixel dithering."),
    ("dragon", "A cute chubby little dragon who loves epic three-hour films.",
     "Baby: a tiny dragon hatchling with stubby wings and a little puff of smoke. Young: small horns and popcorn-shaped spikes along its back. Adult: bigger wings, a glowing ember belly and a tiny film reel hugged under one arm. Ancient: a golden crown of horns, a long tail curled round a pile of golden film cans, smoke rings.",
     "Emerald green scales, cream belly, gold horns, orange ember glow."),
    ("unicorn", "A cute small unicorn who only eats five-star films.",
     "Baby: a tiny foal with a nub of a horn. Young: a pastel rainbow mane. Adult: a spiral golden horn shining like a projector beam and five tiny stars in its mane. Ancient: a flowing rainbow mane and tail, a golden horn and a glowing star on its forehead.",
     "White coat, pastel rainbow mane, gold horn, small pink hooves."),
    ("phoenix", "A cute round phoenix chick who comes back to the cinema after long breaks, reborn from popcorn embers.",
     "Baby: a tiny fluffy chick with a small flame tuft. Young: flame-tipped wings. Adult: a crest of fire feathers and a long burning tail. Ancient: a magnificent tail of golden fire and a halo of sparks.",
     "Red, orange and gold feathers, glowing yellow flames."),
    ("kitsune", "A cute magical fox spirit (kitsune) who loves films from Japan and East Asia.",
     "Baby: a tiny white fox with one fluffy tail and a red ribbon. Young: two tails and red face markings. Adult: four tails, a little paper lantern and a bell collar. Ancient: nine flowing tails, glowing blue foxfire wisps and a small golden mask on its head.",
     "White fur, red markings, blue foxfire, gold."),
    ("yeti", "A cute fluffy little yeti who watches films all winter under a blanket.",
     "Baby: a tiny white snowball of fur with blue cheeks. Young: a knitted bobble hat. Adult: a long striped scarf and a mug of hot cocoa. Ancient: a huge fluffy coat with icicle tips, a hat with earflaps and snowflakes around it.",
     "Snow white fur, ice blue face, red and white knitwear."),
    ("griffin", "A cute small griffin, half eagle half lion cub, who travels through every decade of cinema.",
     "Baby: a fluffy eaglet-lion cub. Young: small feathered wings. Adult: proud wings and a brass pocket watch on a chain. Ancient: great golden wings, a mane of feathers and a tiny hourglass amulet.",
     "Tawny gold fur, white feathered head, brown wings, brass details."),
    ("golem", "A cute small golem made of clay and stacked film cans, who eats every film there is.",
     "Baby: a tiny round clay golem with one film can on its belly. Young: arms made of rolled film strips. Adult: a body of stacked silver film cans with moss on top. Ancient: a big friendly golem with glowing runes on its cans and a tiny projector lamp in its chest.",
     "Warm terracotta clay, silver tin, green moss, warm yellow glow."),
    ("robot", "A cute little tin robot who never misses a day of watching.",
     "Baby: a tiny boxy robot with a single antenna. Young: a small screen on its chest showing a play symbol. Adult: a rounded tin body with dials and a film-reel shoulder. Ancient: a polished brass robot with glowing lights and a tiny calendar plate on its chest.",
     "Teal and silver tin, brass rivets, warm yellow lights; its eyes are big round glossy dark eyes like the others."),
    ("fairy", "A cute tiny fairy who loves films watched as a couple, a date-night sprite.",
     "Baby: a tiny round sprite with little dragonfly wings. Young: a flower-petal dress. Adult: shimmering wings and a little wand with a heart-shaped star. Ancient: large iridescent wings, a crown of tiny flowers and a trail of sparkles.",
     "Peach skin, pink petal dress, iridescent lilac wings, gold sparkles."),
    ("popkin", "A cute living popcorn bucket creature with a face on the bucket and tiny arms, who loves films rated four popcorn and more.",
     "Baby: a tiny striped paper cup with one popcorn kernel on top. Young: a small striped bucket with a heap of popcorn like hair. Adult: a big striped bucket overflowing with buttery popcorn and a bow tie. Ancient: a golden striped bucket with a crown of popcorn and a cherry on top.",
     "Red and white stripes, golden buttery popcorn, cream."),
    ("gargoyle", "A cute small stone gargoyle who perches on the cinema roof and watches films on weekends.",
     "Baby: a tiny round grey stone imp with stubby wings. Young: little horns and a mossy patch. Adult: bat-like stone wings and a cinema ticket held in its paws. Ancient: weathered stone with amber glowing cracks, ivy and a small bell.",
     "Cool grey stone, green moss, amber glow."),
    ("sphinx", "A cute little sphinx, an Egyptian cat-lion with a pharaoh headdress, who loves the oldest films.",
     "Baby: a tiny sandy cub with a blue-and-gold striped headcloth. Young: a gold collar. Adult: small feathered wings and a scarab amulet shaped like a film reel. Ancient: a full pharaoh headdress, golden wings and a tiny pyramid floating above it.",
     "Sandy gold fur, lapis blue and gold stripes."),
    ("wisp", "A cute will-o-the-wisp, a little flame spirit that lights the way to short films.",
     "Baby: a tiny round blue flame with a face. Young: a little glass lantern around it. Adult: a lantern held up by its small flame arms and swirling sparks. Ancient: a large glowing spirit with a halo of tiny lanterns.",
     "Soft blue and teal flame, white core, brass lantern."),
    # The anime shelf: creatures of Japanese folklore and things from a Japanese home, each fond of anime in its own way.
    ("tanuki", "A cute chubby tanuki (Japanese raccoon dog), a cheerful anime fan with a round cream belly and a green leaf on its head.",
     "Baby: a tiny tanuki cub with a small green leaf on its head. Young: a straw hat hanging on its back and a paper fan. Adult: a short blue festival happi coat, the leaf on its head and a tiny film reel hugged like a drum. Ancient: a big straw hat, a gourd flask on a rope, grey whiskers and a paper lantern.",
     "Warm brown fur, dark eye patches (eyes still big and visible), cream belly, green leaf, indigo blue coat."),
    ("koi", "A cute round koi carp floating in the air like a festival carp streamer, who loves full-length anime films.",
     "Baby: a tiny orange-and-white koi fry. Young: flowing fins and a red spot on its forehead. Adult: long whiskers, streamer-like fins with a film-strip edge and a little splash of water drops. Ancient: a golden koi with long flowing fins, a pearl and small swirling clouds, almost a dragon.",
     "White body with orange-red and black patches, gold scales, pale aqua water drops."),
    ("kappa", "A cute little kappa (Japanese river imp, turtle-like) who watches anime series episode after episode.",
     "Baby: a tiny green kappa with a small water dish on its head and a little shell. Young: a lily-pad collar and a cucumber snack in its hand. Adult: a bigger shell, a TV remote and a bowl of cucumber slices like popcorn. Ancient: a mossy shell with tiny lotus flowers, a long white beard and a little lantern.",
     "Fresh green skin, yellow beak-like mouth, brown shell, blue water dish, pale yellow belly."),
    ("nekomata", "A cute nekomata (Japanese two-tailed spirit cat) who watches anime late at night.",
     "Baby: a tiny black kitten with a forked tail tip. Young: two tails and a small bell on a red collar. Adult: two long tails with blue flame tips and a crescent-moon charm. Ancient: a night-blue haori cloak with stars, two flaming tails and a floating paper lantern.",
     "Black and deep indigo fur, white muzzle and socks, red collar, gold bell, blue spirit flames; its eyes are big round glossy dark eyes like the others, set in a lighter face patch."),
    ("tengu", "A cute little crow tengu (Japanese mountain spirit, a round black crow chick in a monk's outfit) who loves old classic anime.",
     "Baby: a fluffy black crow chick with a tiny black cap. Young: a small feather fan in its wing. Adult: white pompoms on its chest, wooden sandals and an old film reel under its wing. Ancient: big black wings, a red-and-gold monk's robe, a long feather fan and a white beard tuft.",
     "Glossy blue-black feathers, a lighter grey face so the eyes show, orange beak, white pompoms, red and gold robe."),
    ("slime", "A cute jelly slime creature, a wobbly round drop with a face, who loves brand-new anime of the current season.",
     "Baby: a tiny mint-green jelly drop with a shine spot. Young: a little sprout on top and headphones. Adult: a bigger glossy slime with a tiny golden crown and sparkles inside it like stars. Ancient: a large shimmering slime with a jewelled crown, a cape and little bubbles floating around.",
     "Mint green and aqua jelly, white shine, gold crown, pink headphones."),
    ("mecha", "A cute chibi toy giant robot, a round little super-robot hero who loves long anime marathons.",
     "Baby: a tiny round wind-up toy robot with a key on its back. Young: stubby rocket fists and a horned samurai-style helmet crest. Adult: shoulder armour, a red scarf and a glowing chest plate shaped like a film reel. Ancient: a golden-trimmed hero robot with small wings, a long scarf and a glowing star on its chest.",
     "White, royal blue and red armour, yellow trim, cyan glow; its eyes are big round glossy dark eyes like the others."),
    ("maneki", "A cute maneki-neko, a lucky beckoning cat figurine come alive, one paw raised, who loves anime rated four popcorn and more.",
     "Baby: a tiny white round kitten with one paw raised and a red collar. Young: a golden bell and red ears. Adult: a red bib, a big golden coin held against its belly and calico spots. Ancient: a golden lucky cat with a jewelled collar, a large coin and a tiny red cushion under it.",
     "White porcelain-like fur, calico orange and black spots, red collar and ears, gold bell and coin."),
    ("daruma", "A cute round daruma doll creature with a face and tiny feet, who loves rewatching favourite anime again and again.",
     "Baby: a tiny round red daruma with a simple face. Young: golden swirls painted on its sides and bushy painted brows. Adult: a headband, tiny arms holding a round rewind-arrow charm and gold patterns. Ancient: a large gold-and-red daruma with a white beard, a little shrine rope around it and a tiny film reel on top.",
     "Bright red body, cream face, gold patterns, black brows; its eyes are big round glossy dark eyes like the others."),
    ("crane", "A cute origami paper crane come alive, floating, who loves anime it has written a few words about.",
     "Baby: a tiny folded white paper crane with a face. Young: patterned paper wings with a red sun dot. Adult: wings of washi paper with wave patterns and a small ink brush tucked under its wing. Ancient: a magnificent crane of golden patterned paper with a long ribbon tail and floating tiny paper stars.",
     "White and cream paper, red crown spot, indigo wave pattern, gold edges."),
    ("shiba", "A cute fluffy shiba inu dog with a curly tail, a loyal fan who watches anime day after day without a gap.",
     "Baby: a tiny shiba puppy with a curly tail. Young: a green scarf with white swirls round its neck. Adult: the scarf, a rope collar with a small calendar charm and a cinema ticket in its mouth. Ancient: a calm old shiba with a grey muzzle, a festival coat and a little bronze bell.",
     "Orange-tan fur, cream cheeks, chest and tail underside, green scarf, red rope."),
    ("oni", "A cute little oni (Japanese ogre child), round and friendly, who loves anime at weekends.",
     "Baby: a tiny red oni toddler with one little horn and a fang. Young: two horns and tiger-striped shorts. Adult: curly dark hair, a small soft toy club and a popcorn box. Ancient: big golden horns, a tiger-striped cape, festival beads and a paper lantern.",
     "Coral red skin, cream horns, yellow and black tiger stripes, dark navy hair."),
    ("onigiri", "A cute living onigiri rice ball with a face, tiny arms and feet and a strip of seaweed, who loves anime watched with friends.",
     "Baby: a tiny triangle rice ball with a small seaweed strip. Young: a pickled plum dot on top like a hat. Adult: a seaweed wrap like a jacket, chopsticks on its back and a small lunch box of snacks to share. Ancient: a big rice ball with a headband, a seaweed cape and a tea cup.",
     "White rice with soft grey shading, dark green-black seaweed, red plum, warm wood browns."),
    ("kirin", "A cute small kirin (East Asian unicorn-deer with scales and a flowing mane) who only eats five-star anime.",
     "Baby: a tiny fawn with scales on its back and a nub of a horn. Young: a wavy golden mane and small antlers. Adult: a single branching horn, cloud-shaped fur on its hooves and five tiny stars in its mane. Ancient: a golden mane like flames, jade scales, a glowing horn and small clouds under its hooves.",
     "Jade green scales, cream belly, golden mane and horn, soft white clouds."),
    ("ryu", "A cute little eastern dragon (long snake-like body, whiskers, antlers, no wings) curled up, who loves long anime films of two hours and more.",
     "Baby: a tiny blue dragon curled in a ring with stubby whiskers. Young: small antlers and a pearl held in its claws. Adult: a longer coiled body, cloud wisps and a film strip wound round its tail. Ancient: a majestic coiled dragon with long whiskers, golden antlers, a glowing pearl and clouds.",
     "Azure blue scales, cream belly, gold antlers and whiskers, white clouds, glowing pearl."),
    ("suzume", "A cute round sparrow, a fluffy little bird who watches anime in the morning over breakfast.",
     "Baby: a tiny round brown sparrow chick. Young: a little rice-straw hat. Adult: a small yellow round school cap and a little red school backpack on its back, a ticket stub tucked under one wing; nothing in its beak, the face fully clear. Ancient: a plump old sparrow with a kimono collar, a tea cup and a tiny sunrise fan.",
     "Chestnut brown cap and wings, cream belly, black cheek spots, warm yellow and red accents."),
    ("baku", "A cute baku (Japanese dream-eating spirit, like a small tapir with a short trunk), floating on a little cloud, who waits for you to come back to anime after a break.",
     "Baby: a tiny round tapir calf with a short trunk and white stripes. Young: a small dream cloud under it. Adult: a night-blue saddle cloth with stars, tiny tusks and a sleeping cap. Ancient: a starry-coated baku with golden tusks, swirling dream clouds and a crescent moon charm.",
     "Indigo and lavender body, white belly band, pink trunk tip, gold stars, soft cloud white."),
]

PET_HEAD = """Use your image generation tool to create ONE image, then save it as PNG with transparency to the path given at the end (create folders if needed). The first attached picture is a sprite sheet from this game: match its style exactly. The second shows the cosy little cinema projection booth the pet lives in.

Pixel art sprite sheet for a cute cozy pet game about watching films, in a chibi 16-bit style.
One creature shown growing up in FOUR stages in a single horizontal row, left to right: baby, young, adult, ancient.
Each stage is clearly bigger and more detailed than the one before; all four stand on the same baseline, evenly spaced, with empty space between them – they must not touch or overlap.
Detailed fine pixel art (like 128x128 game sprites, not chunky), a crisp thick dark outline, soft cel shading with 3-4 tones per colour, rich saturated colours.
Big round glossy dark eyes, each with a bright white highlight; pink blush cheeks; a happy smile.
Full body, facing the viewer in a slight three-quarter view, sitting or standing.
Proportions: a big head and a small round body; the paws, hands and feet are SMALL and neat, tucked in close to the body – never oversized, chunky or big-footed.
Transparent background. No ground shadow, no text, no letters, no frames, no grid, nothing else in the picture.
The creature loves cinema: its film-related details are part of it, and they grow richer with each stage.

"""

EGG_HEAD = """Use your image generation tool to create ONE image, then save it as PNG to the path given at the end (create folders if needed). The first attached picture is a pet sprite from this game: match its style – detailed fine pixel art (like a 128x128 game sprite, not chunky), crisp dark outline, soft cel shading with 3-4 tones per colour, rich cosy colours, warm lamp-lit mood.

{n} closed round metal film cans in a 3x3 grid, read left to right, row by row. Each can is a magical egg a cinema pet hatches from: a round flat tin film can seen from the front at a slight angle so its lid and side both show, standing upright on its edge like a wheel, with a lid, a rim and a paper label. Each can is coloured and decorated for the creature sleeping inside, as listed. All the same size, centred in their cells, with wide empty space between them: they must not touch.
Pure white background. No text, no letters, no numbers, no shadows on the ground, no frames, no grid lines, nothing else in the picture.

"""

EGGS = {
    "cat": "slate blue tin, a grey fedora silhouette on the label, a tiny moon",
    "fox": "orange tin, a white-tipped tail curling round it, a pencil on the label",
    "owl": "brown tin with feather speckles, a pair of round glasses on the label",
    "bat": "purple tin, little bat wings on its sides, a red moon on the label",
    "coon": "red-and-white striped tin like a popcorn box, a black mask stripe",
    "hamster": "silver tin with portholes, stars and a ringed planet on the label",
    "armadillo": "sandy tin with banded ridges, a sheriff's star on the label",
    "lovebird": "pastel pink tin with hearts and a mint ribbon",
    "monkey": "khaki tin with a map label and a compass rose",
    "frog": "green tin with gold sequins and a little black top hat on top",
    "moth": "night-blue tin with stars and a pale moon, soft glow",
    "sloth": "cosy brown tin wrapped in a red-and-cream knitted band",
    "penguin": "sepia black-and-white tin like an old film, a bowler hat label",
    "redpanda": "rusty red tin covered in colourful travel stickers",
    "pug": "fawn tin with red and blue 3D-glasses stripes",
    "mouse": "grey tin with a red usher's band and a ticket stub label",
    "goldfish": "aqua tin with orange fish-scale pattern and bubbles",
    "squirrel": "chestnut tin with acorn pattern and an acorn on the label",
    "ghost": "pale lavender misty tin, a little ghost silhouette on the label",
    "dragon": "emerald tin with gold scales and a little curl of smoke",
    "unicorn": "white tin with pastel rainbow stripes and five golden stars",
    "phoenix": "red-gold tin with flame patterns and a glowing seam",
    "kitsune": "white tin with red markings, a red rope bow and a paper charm",
    "yeti": "icy blue tin with snowflakes and a frosty rim",
    "griffin": "brass tin with feather engraving and an hourglass label",
    "golem": "terracotta clay tin with moss and carved runes",
    "robot": "teal metal tin with rivets, dials and a small antenna",
    "fairy": "pink petal-covered tin with tiny sparkles",
    "popkin": "red-and-white striped tin with popcorn spilling from the lid",
    "gargoyle": "grey stone tin with ivy and a little stone wing",
    "sphinx": "gold and lapis blue tin with an eye on the label",
    "wisp": "dark teal tin with a soft blue glow leaking from the lid seam",
    "tanuki": "warm brown tin with a green leaf on top and a straw rope band",
    "koi": "white tin with orange-red and black koi patches and little waves",
    "kappa": "green tin with a turtle-shell pattern and a blue water dish on top",
    "nekomata": "black tin with two little blue flames and a golden bell",
    "tengu": "black-feathered tin with white pompoms and a feather fan on the label",
    "slime": "glossy mint-green jelly-like tin with a small golden crown",
    "mecha": "white, blue and red armoured tin with yellow trim and a horned crest",
    "maneki": "white porcelain tin with a red collar, a gold bell and a gold coin on the label",
    "daruma": "bright red tin with gold swirls and bushy painted brows",
    "crane": "tin wrapped in folded cream paper with indigo waves and a red dot",
    "shiba": "orange-tan tin with a green scarf with white swirls tied round it",
    "oni": "coral red tin with two little cream horns and a tiger-striped band",
    "onigiri": "white rice-textured tin with a dark seaweed strip and a red plum on top",
    "kirin": "jade-green scaled tin with a golden mane fringe and five stars",
    "ryu": "azure blue scaled tin with gold whiskers, clouds and a pearl on the label",
    "suzume": "chestnut and cream tin with a sunrise on the label and a straw hat on top",
    "baku": "indigo tin with gold stars, a crescent moon and a soft cloud band",
}

BOOTH = {
    "room": """Use your image generation tool to create ONE image, landscape, about 7:6, then save it as PNG to the path given at the end (create folders if needed). The first attached picture is a pet from this game; the second is the old chunky booth it lived in. Redraw the room in the pet's style: detailed fine pixel art (like a 350x300 game scene, not chunky), crisp dark outlines, soft cel shading with 3-4 tones per colour, cosy warm evening mood.

The EMPTY projection booth of a tiny old cinema in a 2D side-view game, seen straight from the front, flat and orthographic, no perspective, filling the whole picture edge to edge:
- The back wall fills the top 57%: dark plum wallpaper with a subtle damask diamond pattern and soft vertical stripes, a slightly darker band along the very top edge like a shadow under the ceiling.
- Then a brass dado rail and dark wooden wainscot panels (a row of square inset panels) down to 80% of the height.
- A wooden plank floor fills the bottom 20%, seen from slightly above.
- On the far right, a narrow strip (the right 10% of the picture) is the side wall, a bit lighter and closer, with a small square brass-framed projection port window at about 35% height whose glass is pitch black.
Completely empty otherwise: no projector, no furniture, no lamps, no pictures, no posters, no people or animals, no text.

Save the image to: art/booth/sheets/room.png
""",
    "things": """Use your image generation tool to create ONE image, then save it as PNG with transparency to the path given at the end (create folders if needed). The first attached picture is a pet from this game: match its style – detailed fine pixel art (like 128x128 game sprites, not chunky), crisp thick dark outline, soft cel shading with 3-4 tones per colour, rich saturated cosy colours, warm lamp-lit evening mood. The second is the old chunky booth these things stood in, for what they are.

Sprite sheet of FOUR separate objects for a tiny cinema projection booth in a 2D side-view game. All four in a single horizontal row, left to right in the order listed, evenly spaced, with clear empty space between them: they must not touch or overlap. Each object seen from the side/front like furniture in a side-view room, drawn complete and alone.
Transparent background. No floor, no wall, no ground shadow, no text, no letters, no labels, no frames around them, no grid, nothing else in the picture.

1. A vintage 35mm cinema film projector on a tall single-post cast-iron stand with a heavy tripod foot: a dark green-teal metal body facing RIGHT, a brass lens pointing right, a lamp house with cooling slits at its back (left), a small brass name plate. The two film reels are NOT drawn: instead two thin empty reel arms stick up from the top (one at the back-left, one at the front-right). About 1.2 times taller than wide.
2. A red-and-gold vintage popcorn cart with a glass cabinet full of golden popcorn, a little red dome roof, two small black wheels. Taller than wide.
3. A plush red velvet cinema armchair with golden trim, seen from the front, cushioned seat and armrests. Slightly wider than tall.
4. An ornate golden picture frame for a film poster, upright, taller than wide (2:3), with a fully flat bright MAGENTA (#FF00FF) rectangle filling the inside where the picture goes.

Save the image to: art/booth/sheets/things.png
""",
}

HALL = {
    "stage": """Use your image generation tool to create ONE image, landscape 3:1, then save it as PNG to the path given at the end (create folders if needed). The first attached picture is a pet from this game; match its style: detailed fine pixel art (not chunky), crisp dark outlines, soft cel shading with 3-4 tones per colour, cosy warm mood.

The front of a tiny old cinema hall in a 2D game, seen straight from the front, flat and orthographic, filling the whole picture edge to edge: heavy red velvet theatre curtains pulled open at both sides with golden tassels, a scalloped red pelmet with a gold fringe along the top, and in the middle a wide golden-framed cinema screen whose surface is a fully flat bright MAGENTA (#FF00FF) rectangle (a film will be shown there). Below the screen a narrow dark wooden stage edge along the bottom. The wall around is dark plum. No people, no seats, no text.

Save the image to: art/hall/sheets/stage.png
""",
}


# The pets before the anime shelf: their cans are drawn already, nine to a sheet, so the shelf starts a sheet of its own.
BASE = 32


def egg_sheets():
    """The species on each sheet of cans, in order: eggs1.png, eggs2.png…"""
    ids = [p[0] for p in PETS]
    return [part[i:i + 9] for part in (ids[:BASE], ids[BASE:]) for i in range(0, len(part), 9)]


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def main():
    for pid, what, stages, colours in PETS:
        write(os.path.join(ART, "pets", "prompts", pid + ".txt"),
              PET_HEAD + what + "\n" + stages + "\n" + colours + f"\n\nSave the image to: art/pets/sheets/{pid}.png\n")
    for n, part in enumerate(egg_sheets()):
        body = "\n".join(f"{i + 1}. {EGGS[k]}." for i, k in enumerate(part))
        count = "Nine" if len(part) == 9 else str(len(part))
        write(os.path.join(ART, "eggs", "prompts", f"eggs{n + 1}.txt"), EGG_HEAD.replace("{n}", count) + body + f"\n\nSave the image to: art/eggs/sheets/eggs{n + 1}.png\n")
    for k, v in BOOTH.items():
        write(os.path.join(ART, "booth", "prompts", k + ".txt"), v)
    for k, v in HALL.items():
        write(os.path.join(ART, "hall", "prompts", k + ".txt"), v)
    print(len(PETS), "pets,", len(egg_sheets()), "egg sheets,", len(BOOTH), "booth,", len(HALL), "hall")


if __name__ == "__main__":
    main()
