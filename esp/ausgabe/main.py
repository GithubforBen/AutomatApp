# Startet das Programm der Ausgabe (app.py).
#
# Schlägt das fehl - auch schon beim Laden, z.B. MemoryError -, hält crash.py
# den Fehler fest (crash.txt) und zeigt ihn unter http://<Ausgabe>/crash, bis
# der ESP neu startet. Nach einem Update holt der Automat ihn dort ab und
# schreibt ihn in automat.log - sonst rollte boot.py die neue Version still
# zurück, und niemand erführe, warum.
#
# Strg+C (USB) wird nicht abgefangen.

try:
    import app
    app.main()
except Exception as e:
    import crash
    crash.handle(e)
