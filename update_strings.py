import os
import re

files_to_update = [
    '/app/app/src/main/java/com/smokingtracker/ui/RegistrationScreen.kt',
    '/app/app/src/main/java/com/smokingtracker/ui/HomeScreen.kt',
    '/app/app/src/main/java/com/smokingtracker/ui/GraphScreen.kt',
    '/app/app/src/main/java/com/smokingtracker/ui/ProfileScreen.kt',
    '/app/app/src/main/java/com/smokingtracker/ui/TimePickerDialog.kt',
    '/app/app/src/main/java/com/smokingtracker/ui/Navigation.kt'
]

# A basic script to change some hardcoded strings to stringResource() but we can also manually apply patches

