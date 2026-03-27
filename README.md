Read the docs.mc!

## API Dummy Simulation

For development and testing purposes, a dummy external API is provided in `apiDummy/dummyExternal.py`. This script
simulates all four external components: Website, Dispenser, Scanner, and Station.

### Running the Dummies

To run all four simulation instances on ports 8080, 8081, 8082, and 8083:

```bash
python3 apiDummy/dummyExternal.py
```

To run a single instance on a specific port (e.g., 8085):

```bash
python3 apiDummy/dummyExternal.py 8085
```

The Java application expects the following default URLs for its components (can be configured in `config.yaml`):

- Website: `http://127.0.0.1:8080`
- Dispenser: `http://192.168.188.200`
- Scanner: `http://192.168.188.201`
- Station: `http://192.168.188.203` (from `src/main/resources/config.yaml`)
