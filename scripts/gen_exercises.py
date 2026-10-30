import json

M = dict(
    chest="chest", back="back", shoulders="shoulders", biceps="biceps",
    triceps="triceps", quads="quads", hamstrings="hamstrings", glutes="glutes",
    calves="calves", core="core", forearms="forearms", neck="neck", cardio="cardio", fullbody="full body",
)
BAR = "barbell"; DB = "dumbbell"; CAB = "cable"; MAC = "machine"
BW = "bodyweight"; KB = "kettlebell"; BA = "bands"; SM = "smith machine"; EZ = "ez barbell"; WT = "weighted"

X = []
def add(id, name, muscle, secondary, eq, ins):
    X.append(dict(id=id, name=name, muscleGroup=muscle, secondaryMuscles=", ".join(secondary), equipment=eq, instructions=ins))

# ---------- Chest ----------
add("bench-press-barbell", "Barbell Bench Press", M["chest"], [M["triceps"], M["shoulders"]], BAR,
    "Lie on a bench, grip slightly wider than shoulder width, lower the bar to your mid chest, then press back up until your arms are extended.")
add("bench-press-dumbbell", "Dumbbell Bench Press", M["chest"], [M["triceps"], M["shoulders"]], DB,
    "Lie on a bench holding a dumbbell in each hand at chest level, press both up until arms are extended, then lower with control.")
add("bench-press-incline-barbell", "Incline Barbell Bench Press", M["chest"], [M["shoulders"], M["triceps"]], BAR,
    "Set the bench to a 30-45 degree incline and press the bar from your upper chest back up to full extension.")
add("bench-press-incline-dumbbell", "Incline Dumbbell Bench Press", M["chest"], [M["shoulders"], M["triceps"]], DB,
    "On an incline bench, press two dumbbells from upper chest height up and slightly together, then lower with control.")
add("bench-press-decline-barbell", "Decline Barbell Bench Press", M["chest"], [M["triceps"]], BAR,
    "On a decline bench, lower the bar to your lower chest and press back up to full extension.")
add("dumbbell-fly", "Dumbbell Fly", M["chest"], [M["shoulders"]], DB,
    "Lie flat with arms extended above your chest and a slight elbow bend, open your arms out wide, then bring them back together in an arc.")
add("dumbbell-fly-incline", "Incline Dumbbell Fly", M["chest"], [M["shoulders"]], DB,
    "From an incline bench, open your arms out wide with a slight elbow bend and bring them back together above your upper chest.")
add("cable-fly", "Cable Fly", M["chest"], [M["shoulders"]], CAB,
    "Stand between two high pulleys and bring the handles together in front of your chest in a hugging arc, then return slowly.")
add("cable-fly-low-to-high", "Low to High Cable Fly", M["chest"], [M["shoulders"]], CAB,
    "Set pulleys low and sweep the handles up and together in front of your chest, emphasizing the upper chest.")
add("cable-crossover", "Cable Crossover", M["chest"], [M["shoulders"]], CAB,
    "From high pulleys, cross the handles over each other in front of your body, squeezing the chest at the end of the motion.")
add("push-up", "Push Up", M["chest"], [M["triceps"], M["core"]], BW,
    "In a high plank with hands shoulder width apart, lower your chest to the floor and press back up keeping your body straight.")
add("push-up-incline", "Incline Push Up", M["chest"], [M["triceps"]], BW,
    "Perform a push up with your hands elevated on a bench or box to reduce the load.")
add("push-up-decline", "Decline Push Up", M["chest"], [M["shoulders"], M["triceps"]], BW,
    "Perform a push up with your feet elevated on a bench to shift load to the upper chest and shoulders.")
add("dip-chest", "Chest Dip", M["chest"], [M["triceps"], M["shoulders"]], BW,
    "Lean your torso forward on parallel bars and lower until your shoulders are below your elbows, then press back up.")
add("machine-chest-press", "Machine Chest Press", M["chest"], [M["triceps"]], MAC,
    "Sit at the chest press machine, grip the handles at chest height, and press forward until your arms are extended.")
add("pec-deck", "Pec Deck Fly", M["chest"], [], MAC,
    "Sit with elbows at chest height against the pads and squeeze the arms together in front of your chest, then return slowly.")
add("smith-bench-press", "Smith Machine Bench Press", M["chest"], [M["triceps"]], SM,
    "Press the fixed barbell from your mid chest back up to full extension on the Smith machine.")
add("landmine-press", "Landmine Press", M["chest"], [M["shoulders"]], BAR,
    "Press the anchored barbell up and forward from shoulder height while standing, driving with your upper chest.")
add("pullover-dumbbell", "Dumbbell Pullover", M["chest"], [M["back"]], DB,
    "Lie across a bench and sweep a dumbbell from over your chest back behind your head in an arc, then return.")

# ---------- Back ----------
add("deadlift", "Deadlift", M["back"], [M["hamstrings"], M["glutes"], M["quads"]], BAR,
    "Hinge at the hips with a flat back and lift the bar from the floor by extending your hips and knees to a standing position.")
add("deadlift-sumo", "Sumo Deadlift", M["back"], [M["glutes"], M["quads"]], BAR,
    "Set a wide stance with toes out, grip inside your knees, and extend hips and knees to stand tall.")
add("deadlift-romanian", "Romanian Deadlift", M["hamstrings"], [M["glutes"], M["back"]], BAR,
    "With soft knees, hinge at the hips lowering the bar along your thighs until you feel a hamstring stretch, then drive your hips forward to stand.")
add("pull-up", "Pull Up", M["back"], [M["biceps"]], BW,
    "Hang from a bar with an overhand grip slightly wider than shoulders and pull your chin above the bar.")
add("pull-up-chin-up", "Chin Up", M["biceps"], [M["back"]], BW,
    "Hang with an underhand, shoulder width grip and pull your chin above the bar, then lower with control.")
add("pull-up-neutral-grip", "Neutral Grip Pull Up", M["back"], [M["biceps"]], BW,
    "Hang from parallel handles with palms facing each other and pull your chin above your hands.")
add("lat-pulldown", "Lat Pulldown", M["back"], [M["biceps"]], CAB,
    "Sit with thighs secured, pull the bar down to your upper chest by driving your elbows down, then return slowly.")
add("lat-pulldown-close-grip", "Close Grip Lat Pulldown", M["back"], [M["biceps"]], CAB,
    "Use a narrow, neutral grip handle and pull it down to your chest, squeezing your lats.")
add("bent-over-row-barbell", "Barbell Bent Over Row", M["back"], [M["biceps"]], BAR,
    "Hinge to about 45 degrees with a flat back, pull the bar to your lower ribs, then lower with control.")
add("bent-over-row-dumbbell", "Dumbbell Bent Over Row", M["back"], [M["biceps"]], DB,
    "Hinge forward and row both dumbbells up to your ribs, keeping elbows close to your body.")
add("one-arm-row-dumbbell", "One Arm Dumbbell Row", M["back"], [M["biceps"]], DB,
    "Brace one hand on a bench and row a dumbbell up to your hip with the other, then lower fully.")
add("chest-supported-row-machine", "Machine Chest Supported Row", M["back"], [M["biceps"]], MAC,
    "With your chest against the pad, row the handles to your torso and squeeze your shoulder blades together.")
add("seated-cable-row", "Seated Cable Row", M["back"], [M["biceps"]], CAB,
    "Pull the handle to your torso with upright posture, then let your arms extend fully forward.")
add("t-bar-row", "T-Bar Row", M["back"], [M["biceps"]], BAR,
    "Straddle the anchored bar, hinge forward, and row the weight up to your chest.")
add("pendlay-row", "Pendlay Row", M["back"], [M["hamstrings"]], BAR,
    "From a dead stop on the floor each rep, explosively row the bar to your lower chest with a flat back.")
add("face-pull", "Face Pull", M["shoulders"], [M["back"]], CAB,
    "Pull a rope attachment toward your face, splitting your hands beside your ears, keeping elbows high.")
add("straight-arm-pulldown", "Straight Arm Pulldown", M["back"], [M["core"]], CAB,
    "With arms nearly straight, sweep the bar from shoulder height down to your thighs using your lats.")
add("shrug-barbell", "Barbell Shrug", M["back"], [], BAR,
    "Hold the bar at hip level and raise your shoulders straight up toward your ears, then lower slowly.")
add("shrug-dumbbell", "Dumbbell Shrug", M["back"], [], DB,
    "Hold dumbbells at your sides and raise your shoulders toward your ears without bending your elbows.")
add("good-morning", "Good Morning", M["hamstrings"], [M["back"], M["glutes"]], BAR,
    "With the bar on your upper back and soft knees, hinge at the hips until your torso is near horizontal, then stand back up.")
add("hyperextension", "Back Extension", M["back"], [M["glutes"], M["hamstrings"]], BW,
    "On a hyperextension bench, lower your torso with control and raise back to a straight line with your legs.")
add("inverted-row", "Inverted Row", M["back"], [M["biceps"], M["core"]], BW,
    "Under a fixed bar, hang with a straight body and row your chest up to the bar.")

# ---------- Shoulders ----------
add("overhead-press-barbell", "Barbell Overhead Press", M["shoulders"], [M["triceps"]], BAR,
    "Press the bar from shoulder height overhead until your arms are locked out, keeping your core braced.")
add("overhead-press-dumbbell", "Dumbbell Shoulder Press", M["shoulders"], [M["triceps"]], DB,
    "Press two dumbbells from shoulder height overhead until your arms are extended.")
add("overhead-press-seated-dumbbell", "Seated Dumbbell Shoulder Press", M["shoulders"], [M["triceps"]], DB,
    "Seated with back support, press dumbbells from shoulder height straight up and lower with control.")
add("arnold-press", "Arnold Press", M["shoulders"], [M["triceps"]], DB,
    "Start with palms facing you at shoulder height and rotate your palms outward as you press overhead.")
add("lateral-raise-dumbbell", "Dumbbell Lateral Raise", M["shoulders"], [], DB,
    "With a slight elbow bend, raise dumbbells out to your sides to shoulder height, then lower slowly.")
add("lateral-raise-cable", "Cable Lateral Raise", M["shoulders"], [], CAB,
    "Raise the low pulley handle out to your side to shoulder height, keeping your arm nearly straight.")
add("front-raise-dumbbell", "Dumbbell Front Raise", M["shoulders"], [], DB,
    "Raise one or both dumbbells straight in front of you to shoulder height, then lower with control.")
add("rear-delt-fly-dumbbell", "Dumbbell Rear Delt Fly", M["shoulders"], [M["back"]], DB,
    "Hinge forward and raise dumbbells out wide with a slight elbow bend, squeezing your rear delts.")
add("rear-delt-fly-machine", "Machine Rear Delt Fly", M["shoulders"], [M["back"]], MAC,
    "With chest against the pad, sweep your arms back and open until level with your shoulders.")
add("rear-delt-row-cable", "Cable Rear Delt Row", M["shoulders"], [M["back"]], CAB,
    "Row the rope toward your upper chest with elbows wide, focusing on your rear delts.")
add("upright-row-barbell", "Barbell Upright Row", M["shoulders"], [M["back"]], BAR,
    "Pull the bar up along your body to chest height with elbows leading, then lower slowly.")
add("plate-front-raise", "Plate Front Raise", M["shoulders"], [], WT,
    "Hold a weight plate with both hands and raise it straight ahead to eye level, then lower.")
add("band-face-pull", "Band Face Pull", M["shoulders"], [M["back"]], BA,
    "Anchor the band at face height, pull it toward your face and split your hands beside your ears.")

# ---------- Biceps ----------
add("barbell-curl", "Barbell Curl", M["biceps"], [M["forearms"]], BAR,
    "Curl the bar from hip height to shoulder height keeping your elbows pinned at your sides.")
add("dumbbell-curl", "Dumbbell Curl", M["biceps"], [M["forearms"]], DB,
    "Curl both dumbbells up, rotating your palms to face up as you lift.")
add("dumbbell-curl-alternate", "Alternating Dumbbell Curl", M["biceps"], [M["forearms"]], DB,
    "Curl one dumbbell at a time, alternating arms while keeping your torso still.")
add("hammer-curl", "Hammer Curl", M["biceps"], [M["forearms"]], DB,
    "Curl dumbbells with palms facing each other throughout the movement.")
add("preacher-curl", "Preacher Curl", M["biceps"], [], MAC,
    "With upper arms against the preacher pad, curl the weight up and lower slowly to full extension.")
add("concentration-curl", "Concentration Curl", M["biceps"], [], DB,
    "Seated with your elbow braced against your inner thigh, curl the dumbbell up and squeeze at the top.")
add("incline-curl", "Incline Dumbbell Curl", M["biceps"], [], DB,
    "Lie back on an incline bench with arms hanging and curl both dumbbells through a long range.")
add("spider-curl", "Spider Curl", M["biceps"], [], DB,
    "Chest down on an incline bench, curl both dumbbells with your arms hanging vertically.")
add("ez-bar-curl", "EZ Bar Curl", M["biceps"], [M["forearms"]], EZ,
    "Curl the EZ bar up with a shoulder width grip, keeping elbows fixed at your sides.")
add("cable-curl", "Cable Curl", M["biceps"], [M["forearms"]], CAB,
    "Curl the low pulley bar up keeping constant tension through the whole range.")
add("rope-hammer-curl", "Rope Hammer Curl", M["biceps"], [M["forearms"]], CAB,
    "Curl the rope handles with palms facing each other, splitting them at the top.")
add("band-curl", "Band Curl", M["biceps"], [], BA,
    "Stand on the band and curl your hands up, keeping tension in the band at all times.")

# ---------- Triceps ----------
add("close-grip-bench-press", "Close Grip Bench Press", M["triceps"], [M["chest"]], BAR,
    "Grip the bar at shoulder width, lower to your lower chest, and press back up driving with your triceps.")
add("skull-crusher", "Barbell Skull Crusher", M["triceps"], [], BAR,
    "Lie on a bench and lower the bar toward your forehead by bending your elbows, then extend back up.")
add("skull-crusher-dumbbell", "Dumbbell Skull Crusher", M["triceps"], [], DB,
    "Lower two dumbbells beside your head by bending the elbows, then extend back up.")
add("triceps-dip", "Triceps Dip", M["triceps"], [M["chest"], M["shoulders"]], BW,
    "On parallel bars keep your torso vertical, lower until elbows reach 90 degrees, and press back up.")
add("bench-dip", "Bench Dip", M["triceps"], [M["chest"]], BW,
    "With hands on a bench behind you, lower your hips toward the floor and press back up.")
add("cable-pushdown", "Cable Triceps Pushdown", M["triceps"], [], CAB,
    "With elbows pinned at your sides, push the bar down until your arms are fully extended.")
add("rope-pushdown", "Rope Triceps Pushdown", M["triceps"], [], CAB,
    "Push the rope down and split your hands apart at the bottom of the movement.")
add("overhead-extension-dumbbell", "Overhead Dumbbell Extension", M["triceps"], [], DB,
    "Lower a dumbbell behind your head by bending your elbows, then extend back up overhead.")
add("overhead-extension-cable", "Overhead Cable Extension", M["triceps"], [], CAB,
    "Facing away from a high pulley, extend the rope overhead until your arms are straight.")
add("kickback", "Triceps Kickback", M["triceps"], [], DB,
    "Hinged forward, extend your forearm back until your arm is straight, squeezing the triceps.")
add("push-up-diamond", "Diamond Push Up", M["triceps"], [M["chest"], M["shoulders"]], BW,
    "Place your hands close together forming a diamond under your chest, lower down, and press back up.")

# ---------- Quads ----------
add("squat-back", "Barbell Back Squat", M["quads"], [M["glutes"], M["core"]], BAR,
    "With the bar on your upper back, squat down until your hips reach knee level or below, then drive up through your heels.")
add("squat-front", "Front Squat", M["quads"], [M["core"]], BAR,
    "Hold the bar on your front delts with elbows high and squat down, keeping an upright torso.")
add("squat-goblet", "Goblet Squat", M["quads"], [M["glutes"], M["core"]], DB,
    "Hold a weight at your chest and squat down between your legs, keeping your chest up.")
add("leg-press", "Leg Press", M["quads"], [M["glutes"]], MAC,
    "Push the platform away until your legs are nearly straight, then lower until your knees reach about 90 degrees.")
add("hack-squat", "Hack Squat", M["quads"], [M["glutes"]], MAC,
    "On the hack sled, lower with control until knees are bent past 90 degrees and press back up.")
add("smith-squat", "Smith Machine Squat", M["quads"], [M["glutes"]], SM,
    "Squat under the fixed bar, keeping your feet slightly forward to sit back into the movement.")
add("lunge", "Walking Lunge", M["quads"], [M["glutes"], M["hamstrings"]], DB,
    "Step forward into a long stride and lower your back knee toward the floor, then step through to the next lunge.")
add("lunge-static", "Static Lunge", M["quads"], [M["glutes"]], DB,
    "From a split stance, lower your back knee straight down and push back up without stepping.")
add("lunge-reverse", "Reverse Lunge", M["quads"], [M["glutes"]], DB,
    "Step backward into a lunge, lowering until both knees are bent 90 degrees, then return to standing.")
add("bulgarian-split-squat", "Bulgarian Split Squat", M["quads"], [M["glutes"]], DB,
    "Rear foot elevated on a bench, lower straight down until your front thigh is parallel, then drive up.")
add("step-up", "Dumbbell Step Up", M["quads"], [M["glutes"]], DB,
    "Step up onto a box driving through your top leg, then lower with control.")
add("leg-extension", "Leg Extension", M["quads"], [], MAC,
    "Extend your knees until your legs are straight, pause, and lower the pad slowly.")
add("sissy-squat", "Sissy Squat", M["quads"], [], BW,
    "Rise to your toes and lean back while bending your knees, keeping hips extended, then return.")
add("wall-sit", "Wall Sit", M["quads"], [M["core"]], BW,
    "Slide down a wall until your thighs are parallel to the floor and hold the position.")
add("pistol-squat", "Pistol Squat", M["quads"], [M["glutes"], M["core"]], BW,
    "Squat down on one leg with the other extended forward, then stand back up.")
add("squat-jump", "Squat Jump", M["quads"], [M["glutes"]], BW,
    "Squat down and explode upward into a jump, landing softly back into a squat.")

# ---------- Hamstrings ----------
add("deadlift-stiff-leg", "Stiff Leg Deadlift", M["hamstrings"], [M["glutes"], M["back"]], BAR,
    "With nearly straight legs, hinge at the hips to lower the bar along your legs, then stand by extending your hips.")
add("leg-curl-lying", "Lying Leg Curl", M["hamstrings"], [], MAC,
    "Face down on the bench, curl the pad toward your glutes and lower slowly.")
add("leg-curl-seated", "Seated Leg Curl", M["hamstrings"], [], MAC,
    "Curl the pad under your seat toward your hamstrings, pause, and return with control.")
add("nordic-curl", "Nordic Curl", M["hamstrings"], [], BW,
    "Kneeling with ankles fixed, lower your torso forward as slowly as possible and pull back up.")
add("glute-ham-raise", "Glute Ham Raise", M["hamstrings"], [M["glutes"]], MAC,
    "From the GHR bench, extend your body backward and curl back up using your hamstrings and glutes.")
add("single-leg-rdl", "Single Leg Romanian Deadlift", M["hamstrings"], [M["glutes"], M["core"]], DB,
    "Hinge forward on one leg while the other extends behind you, keeping a flat back, then stand up.")

# ---------- Glutes ----------
add("hip-thrust", "Barbell Hip Thrust", M["glutes"], [M["hamstrings"]], BAR,
    "With upper back on a bench and the bar over your hips, drive your hips up until your body is level, squeeze, and lower.")
add("glute-bridge", "Glute Bridge", M["glutes"], [M["hamstrings"]], BW,
    "Lying on your back, drive your hips up by squeezing your glutes, then lower with control.")
add("cable-kickback", "Cable Glute Kickback", M["glutes"], [M["hamstrings"]], CAB,
    "Ankle strap at a low pulley, kick your leg straight back and up, squeezing the glute.")
add("hip-abduction", "Hip Abduction", M["glutes"], [], MAC,
    "Push the pads apart with your legs, pause at the widest point, and return slowly.")
add("step-down", "Box Step Down", M["glutes"], [M["quads"]], BW,
    "Stand on a box and lower one foot to the floor with control, then drive back up.")

# ---------- Calves ----------
add("calf-raise-standing", "Standing Calf Raise", M["calves"], [], MAC,
    "Rise onto the balls of your feet as high as possible, pause at the top, and lower into a deep stretch.")
add("calf-raise-seated", "Seated Calf Raise", M["calves"], [], MAC,
    "With the pad on your knees, press through the balls of your feet and lift your heels.")
add("calf-raise-dumbbell", "Dumbbell Calf Raise", M["calves"], [], DB,
    "Holding dumbbells, rise onto your toes on a step or flat floor and lower into a stretch.")
add("calf-raise-leg-press", "Leg Press Calf Raise", M["calves"], [], MAC,
    "On the leg press, push the platform with the balls of your feet and control the return.")
add("donkey-calf-raise", "Donkey Calf Raise", M["calves"], [], MAC,
    "Hinged forward under the pad, raise your heels through a full range.")
add("seated-calf-raise-dumbbell", "Seated Dumbbell Calf Raise", M["calves"], [], DB,
    "With a dumbbell across your knees, raise your heels as high as possible.")
add("tibialis-raise", "Tibialis Raise", M["calves"], [], BW,
    "With heels on a step, pull your toes up toward your shins and lower slowly.")

# ---------- Core ----------
add("plank", "Plank", M["core"], [], BW,
    "Hold a straight line from head to heels on your forearms, bracing your core.")
add("side-plank", "Side Plank", M["core"], [], BW,
    "Balance on one forearm with your body in a straight line and hips lifted.")
add("crunch", "Crunch", M["core"], [], BW,
    "Curl your shoulder blades off the floor by contracting your abs, then lower slowly.")
add("sit-up", "Sit Up", M["core"], [], BW,
    "From lying with bent knees, sit all the way up and return with control.")
add("hanging-leg-raise", "Hanging Leg Raise", M["core"], [], BW,
    "Hang from a bar and raise your legs to hip height or above, then lower slowly without swinging.")
add("knee-raise", "Hanging Knee Raise", M["core"], [], BW,
    "Hang from a bar and pull your knees up toward your chest, then lower with control.")
add("leg-raise", "Lying Leg Raise", M["core"], [], BW,
    "Lying flat, raise straight legs to vertical and lower them slowly without touching the floor.")
add("russian-twist", "Russian Twist", M["core"], [], WT,
    "Seated with feet off the floor, rotate your torso side to side touching the weight beside your hip.")
add("cable-crunch", "Cable Crunch", M["core"], [], CAB,
    "Kneeling below a high pulley, crunch your ribs toward your hips and return with control.")
add("ab-wheel", "Ab Wheel Rollout", M["core"], [], WT,
    "Roll the wheel forward keeping your core braced, then pull back to the start.")
add("dead-bug", "Dead Bug", M["core"], [], BW,
    "On your back, extend opposite arm and leg while keeping your lower back pressed to the floor.")
add("mountain-climber", "Mountain Climber", M["core"], [M["cardio"]], BW,
    "In a plank, drive your knees alternately toward your chest at speed.")
add("hollow-hold", "Hollow Body Hold", M["core"], [], BW,
    "Press your lower back down and hold your shoulders and legs just off the floor.")
add("side-bend-dumbbell", "Dumbbell Side Bend", M["core"], [], DB,
    "Holding a dumbbell on one side, bend sideways to lower it and pull back up with your obliques.")
add("medicine-ball-slam", "Medicine Ball Slam", M["core"], [M["cardio"]], WT,
    "Raise the ball overhead and slam it down with full force, hinging at the hips.")

# ---------- Forearms / Grip ----------
add("wrist-curl", "Wrist Curl", M["forearms"], [], DB,
    "Forearms on a bench, curl your wrists up and lower slowly.")
add("reverse-wrist-curl", "Reverse Wrist Curl", M["forearms"], [], DB,
    "Palms down on a bench, extend your wrists up and lower slowly.")
add("farmer-carry", "Farmer Carry", M["forearms"], [M["core"], M["back"]], DB,
    "Walk with heavy weights at your sides, keeping posture tall and grip tight.")
add("dead-hang", "Dead Hang", M["forearms"], [M["back"]], BW,
    "Hang from a bar with straight arms as long as you can.")
add("reverse-curl", "Reverse Curl", M["forearms"], [M["biceps"]], EZ,
    "Curl with palms facing down to target the forearm extensors.")
add("wrist-roller", "Wrist Roller", M["forearms"], [], WT,
    "Roll the handle up and down to wind the cord around it, working your forearms.")
add("plate-pinch-hold", "Plate Pinch Hold", M["forearms"], [], WT,
    "Pinch two plates together at your side and hold for time.")

# ---------- Neck ----------
add("neck-flexion", "Neck Flexion", M["neck"], [], WT,
    "Lying on your back, tuck your chin to your chest against resistance and return.")
add("neck-extension", "Neck Extension", M["neck"], [], WT,
    "Lying face down, raise your head backward against resistance and lower slowly.")

# ---------- Cardio / Full body ----------
add("treadmill-run", "Treadmill Run", M["cardio"], [], MAC,
    "Run at a steady pace on the treadmill, adjusting speed or incline for intensity.")
add("cycling", "Stationary Bike", M["cardio"], [], MAC,
    "Cycle at a steady cadence, adjusting resistance for the desired intensity.")
add("row-erg", "Rowing Machine", M["cardio"], [M["back"]], MAC,
    "Drive with your legs, then pull the handle to your chest in one fluid motion, and recover.")
add("jump-rope", "Jump Rope", M["cardio"], [M["calves"]], BW,
    "Skip rope with light bounces from your ankles, keeping a steady rhythm.")
add("burpee", "Burpee", M["cardio"], [M["fullbody"]], BW,
    "From standing, drop into a push up, jump your feet in, and leap upward.")
add("stair-climber", "Stair Climber", M["cardio"], [M["glutes"]], MAC,
    "Climb at a steady pace keeping your torso upright.")
add("elliptical", "Elliptical", M["cardio"], [], MAC,
    "Move in a smooth elliptical stride, adjusting resistance and incline.")
add("kettlebell-swing", "Kettlebell Swing", M["glutes"], [M["hamstrings"], M["cardio"]], KB,
    "Hike the kettlebell back between your legs and snap your hips forward to swing it to chest height.")
add("kettlebell-clean", "Kettlebell Clean", M["shoulders"], [M["fullbody"]], KB,
    "Pull the kettlebell from the floor to your shoulder in one controlled motion, catching it softly.")
add("kettlebell-snatch", "Kettlebell Snatch", M["shoulders"], [M["fullbody"]], KB,
    "Explosively swing the kettlebell from the floor to a locked out position overhead.")
add("kettlebell-goblet-squat", "Kettlebell Goblet Squat", M["quads"], [M["glutes"]], KB,
    "Hold the kettlebell at your chest and squat between your legs keeping your chest up.")
add("kettlebell-lunge-press", "Kettlebell Overhead Lunge", M["quads"], [M["shoulders"]], KB,
    "Hold a kettlebell overhead and lunge forward, keeping the arm locked out.")
add("clean-and-press", "Clean and Press", M["shoulders"], [M["fullbody"]], BAR,
    "Pull the bar from the floor to your shoulders, then press it overhead in one continuous effort.")
add("power-clean", "Power Clean", M["fullbody"], [M["back"], M["quads"]], BAR,
    "Explosively pull the bar from the floor to your shoulders catching it in a quarter squat.")
add("push-press", "Push Press", M["shoulders"], [M["quads"], M["triceps"]], BAR,
    "Dip slightly at the knees and drive the bar overhead, using leg momentum to press.")
add("thruster", "Thruster", M["quads"], [M["shoulders"], M["fullbody"]], BAR,
    "Squat down with the bar at your shoulders and drive up into an overhead press in one motion.")
add("muscle-up", "Muscle Up", M["back"], [M["chest"], M["core"]], BW,
    "Pull up explosively and transition over the bar, pressing to a support position above it.")
add("box-jump", "Box Jump", M["quads"], [M["glutes"], M["cardio"]], BW,
    "Explode from a quarter squat onto a stable box, landing softly, then step down.")
add("battle-ropes", "Battle Ropes", M["cardio"], [M["shoulders"]], WT,
    "Create rapid alternating waves with the ropes in an athletic stance.")
add("assault-bike", "Assault Bike", M["cardio"], [M["fullbody"]], MAC,
    "Push and pull the handles while pedaling at a self-selected sprint or pace.")
add("sled-push", "Sled Push", M["quads"], [M["fullbody"]], WT,
    "Drive the sled forward with powerful, low, short steps.")

# ---------- More variations ----------
add("pull-up-band-assisted", "Band Assisted Pull Up", M["back"], [M["biceps"]], BA,
    "Loop a band over the bar and under your knees to assist your pull ups.")
add("smith-shoulder-press", "Smith Machine Shoulder Press", M["shoulders"], [M["triceps"]], SM,
    "Press the fixed bar overhead from chin height while seated on the Smith bench.")
add("smith-hip-thrust", "Smith Machine Hip Thrust", M["glutes"], [M["hamstrings"]], SM,
    "Thrust your hips up against the Smith bar positioned over your hips.")
add("band-pull-apart", "Band Pull Apart", M["shoulders"], [M["back"]], BA,
    "Stretch the band apart in front of your chest, squeezing your shoulder blades together.")
add("band-pull-down", "Band Lat Pulldown", M["back"], [M["biceps"]], BA,
    "Anchor the band overhead and pull it down to your chest like a pulldown.")
add("band-chest-press", "Band Chest Press", M["chest"], [M["triceps"]], BA,
    "Anchor the band behind you and press forward until your arms are extended.")
add("band-shoulder-press", "Band Overhead Press", M["shoulders"], [M["triceps"]], BA,
    "Stand on the band and press both hands overhead until your arms lock out.")
add("band-row", "Band Row", M["back"], [M["biceps"]], BA,
    "Anchor the band in front of you and row the ends toward your torso.")
add("kb-around-the-world", "Kettlebell Around the World", M["core"], [], KB,
    "Pass the kettlebell around your waist, switching hands in front and behind.")
add("kb-press", "Kettlebell Press", M["shoulders"], [M["triceps"]], KB,
    "Press the kettlebell overhead from your shoulder, keeping your wrist straight.")
add("single-arm-kb-swing", "Single Arm Kettlebell Swing", M["glutes"], [M["cardio"]], KB,
    "Swing the kettlebell to chest height with one arm, driving with your hips.")
add("kettlebell-row", "Kettlebell Row", M["back"], [M["biceps"]], KB,
    "Hinge forward and row the kettlebell to your hip with a flat back.")
add("cable-lateral-raise-single", "Single Arm Cable Lateral Raise", M["shoulders"], [], CAB,
    "Stand beside a low pulley and raise the handle out to your side to shoulder height.")
add("chest-supported-dumbbell-row", "Chest Supported Dumbbell Row", M["back"], [M["biceps"]], DB,
    "Lying prone on an incline bench, row both dumbbells toward your hips, squeezing your shoulder blades.")
add("smith-row", "Smith Machine Row", M["back"], [M["biceps"]], SM,
    "Hinge under the Smith bar and row it to your torso.")
add("cable-upright-row", "Cable Upright Row", M["shoulders"], [M["back"]], CAB,
    "Pull the low pulley bar up along your body to chest height with elbows leading.")
add("prone-cobra", "Prone Cobra", M["back"], [M["shoulders"]], BW,
    "Lying face down, lift your chest with arms at your sides and rotate your palms out, squeezing your back.")
add("hyperextension-glute", "45 Degree Glute Extension", M["glutes"], [M["hamstrings"]], BW,
    "On a 45 degree bench, extend your hips to raise your torso to a straight line.")
add("band-glute-bridge", "Banded Glute Bridge", M["glutes"], [], BA,
    "With a band above your knees, bridge your hips up pushing your knees out against the band.")
add("clamshell", "Clamshell", M["glutes"], [], BA,
    "Lying on your side with a band above your knees, open your knees apart keeping your feet together.")
add("band-lateral-walk", "Banded Lateral Walk", M["glutes"], [], BA,
    "With a band above your knees, side step in a semi squat keeping tension.")
add("cable-hip-abduction", "Cable Hip Abduction", M["glutes"], [], CAB,
    "Ankle strap at a low pulley, sweep your leg out to the side against the cable.")
add("hamstring-slide-curl", "Slide Leg Curl", M["hamstrings"], [], BW,
    "On sliders, curl your heels toward your glutes by lifting your hips and sliding your feet in.")
add("reverse-hyper", "Reverse Hyperextension", M["glutes"], [M["hamstrings"]], MAC,
    "Swing your legs back and up over the bench using your glutes, not your lower back.")
add("cable-pull-through", "Cable Pull Through", M["glutes"], [M["hamstrings"]], CAB,
    "Between your legs pull the rope through by driving your hips forward to standing.")
add("sumo-squat-dumbbell", "Sumo Squat", M["glutes"], [M["quads"]], DB,
    "Wide stance with toes out, squat straight down holding a weight at your chest.")
add("curtsy-lunge", "Curtsy Lunge", M["glutes"], [M["quads"]], DB,
    "Step one leg behind and across your body into a lunge, then return to standing.")
add("lateral-lunge", "Lateral Lunge", M["quads"], [M["glutes"]], DB,
    "Step wide to one side, sit back into that hip, then push back to center.")
add("hip-flexor-raise", "L-Sit", M["core"], [M["forearms"]], BW,
    "Support yourself on parallel bars and hold your straight legs out in front of you.")
add("scapula-pull", "Scapular Pull Up", M["back"], [], BW,
    "From a dead hang, pull your shoulder blades down without bending your elbows.")
add("shrimp-squat", "Shrimp Squat", M["quads"], [], BW,
    "Balance on one leg holding your other foot behind you and squat down to the floor.")
add("glute-kickback-machine", "Machine Glute Kickback", M["glutes"], [], MAC,
    "Push the lever back and up with your leg, squeezing the glute at the top.")
add("adduction-machine", "Hip Adduction", M["glutes"], [], MAC,
    "Squeeze the pads together in front of you and return slowly.")
add("leg-curl-nordic-band", "Band Assisted Nordic Curl", M["hamstrings"], [], BA,
    "Perform nordic curls with a band under your chest to reduce the load.")

with open('app/src/main/assets/exercises.json', 'w') as f:
    json.dump(X, f, indent=1)
ids = [x["id"] for x in X]
print("exercises:", len(X), "unique ids:", len(set(ids)))
