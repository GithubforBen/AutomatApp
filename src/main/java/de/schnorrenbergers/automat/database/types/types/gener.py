import random

# Erstelle eine Liste mit ca. 980 Gender-Identitäten (kreative, plausible Namen plus Variationen)
base_genders = [
    "Agender", "Androgyne", "Bigender", "Cisgender", "CisFemale", "CisMale",
    "Demiboy", "Demigirl", "Genderfluid", "Genderqueer", "Intergender",
    "Maverique", "Multigender", "Neutrois", "NonBinary", "Omnigender",
    "Pangender", "Polygender", "Transfeminine", "Transmasculine", "Transwoman",
    "Transman", "TransNonBinary", "TwoSpirit", "AFABNonBinary", "AMABNonBinary",
    "FemaleToMale", "MaleToFemale", "ThirdGender", "Novigender", "Autigender",
    "Greygender", "Quoigender", "Agendflux", "Ceterogender", "Gynegender",
    "Lunagender", "Libragender", "Neurogeder", "POCGender", "Systemgender",
    "Xenogender", "Genderfae", "Gendervast", "Demiflux", "Intersex",
    "Astralgender", "Chaosgender", "Cloudgender", "Voidgender"
]

# Zusätzliche kreative Genderidentitäten generieren
prefixes = ["Neo", "Post", "Ultra", "Meta", "Para", "Quasi", "Auto", "Cyber", "Bio", "Chrono", ""]
suffixes = ["gender", "flux", "fluid", "core", "type", "form", "shade", "wave", "mode", "zone", ""]

# Variationen mit "Jan" einfügen
jan_variants = ["Janfluid", "Jangender", "Janflux", "Janform", "Janbinary", "Janby", "Janmasc", "Janfem", "Janqueer",
                "Janfluxian"]

# Generiere zufällige, plausible Gendernamen
generated = set()
for p in prefixes:
    for s in suffixes:
        for b in base_genders:
            generated.add(p + b + s)

# Kombiniere und sortiere die Liste alphabetisch
all_genders = list(generated)
all_genders += jan_variants
all_genders.sort()

# Baue das Java enum
java_enum = "public enum GenderIdentity {\n"
for gender in all_genders:
    java_enum += f"    {gender.upper()},\n"
java_enum = java_enum.rstrip(",\n") + "\n}\n"
print(java_enum)
with open("GenderIdentity.java", "w") as f:
    f.write(java_enum)
