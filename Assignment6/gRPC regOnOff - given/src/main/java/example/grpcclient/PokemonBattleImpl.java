package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PokemonBattleImpl extends PokemonBattleGrpc.PokemonBattleImplBase {

    private static final String POKEMON_FILE = "pokemon_data.json";
    private static final String HISTORY_FILE = "battle_history.json";
    private List<JSONObject> pokemons = new ArrayList<>();
    private List<JSONObject> battleHistory = new ArrayList<>();
    private int nextId = 1;

    public PokemonBattleImpl() {
        super();
        loadData();
    }

    private void loadData() {
        // load saved pokemon from disk
        try {
            if (Files.exists(Paths.get(POKEMON_FILE))) {
                String content = new String(Files.readAllBytes(Paths.get(POKEMON_FILE)));
                JSONArray arr = new JSONArray(content);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject p = arr.getJSONObject(i);
                    pokemons.add(p);
                    int id = p.optInt("id", 0);
                    if (id >= nextId) {
                        nextId = id + 1;
                    }
                }
                System.out.println("Loaded " + pokemons.size() + " pokemon from " + POKEMON_FILE);
            }
        } catch (Exception e) {
            System.out.println("Could not load pokemon data: " + e.getMessage());
        }

        // load saved battle history from disk
        try {
            if (Files.exists(Paths.get(HISTORY_FILE))) {
                String content = new String(Files.readAllBytes(Paths.get(HISTORY_FILE)));
                JSONArray arr = new JSONArray(content);
                for (int i = 0; i < arr.length(); i++) {
                    battleHistory.add(arr.getJSONObject(i));
                }
                System.out.println("Loaded " + battleHistory.size() + " battles from " + HISTORY_FILE);
            }
        } catch (Exception e) {
            System.out.println("Could not load battle history: " + e.getMessage());
        }
    }

    private void savePokemon() {
        try {
            JSONArray arr = new JSONArray();
            for (JSONObject p : pokemons) {
                arr.put(p);
            }
            Files.write(Paths.get(POKEMON_FILE), arr.toString(2).getBytes());
        } catch (Exception e) {
            System.out.println("Could not save pokemon: " + e.getMessage());
        }
    }

    private void saveHistory() {
        try {
            JSONArray arr = new JSONArray();
            for (JSONObject b : battleHistory) {
                arr.put(b);
            }
            Files.write(Paths.get(HISTORY_FILE), arr.toString(2).getBytes());
        } catch (Exception e) {
            System.out.println("Could not save battle history: " + e.getMessage());
        }
    }

    // convert JSON object back to proto Pokemon message
    private Pokemon jsonToPokemon(JSONObject obj) {
        PokemonType type;
        try {
            type = PokemonType.valueOf(obj.optString("type", "UNKNOWN"));
        } catch (IllegalArgumentException e) {
            type = PokemonType.UNKNOWN;
        }
        return Pokemon.newBuilder()
            .setId(obj.optInt("id", 0))
            .setName(obj.optString("name", ""))
            .setType(type)
            .build();
    }

    private JSONObject findByName(String name) {
        for (JSONObject p : pokemons) {
            if (p.optString("name", "").equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    // fire > grass, grass > water, water > fire
    // returns result from p1's perspective
    private String determineResult(PokemonType t1, PokemonType t2) {
        if (t1 == t2) {
            return "You Tie";
        }
        if (t1 == PokemonType.FIRE && t2 == PokemonType.GRASS) {
            return "You Win";
        }
        if (t1 == PokemonType.GRASS && t2 == PokemonType.WATER) {
            return "You Win";
        }
        if (t1 == PokemonType.WATER && t2 == PokemonType.FIRE) {
            return "You Win";
        }
        return "You Lose";
    }

    @Override
    public synchronized void createPokemon(CreatePokemonRequest req, StreamObserver<PokemonResponse> responseObserver) {
        System.out.println("Received: createPokemon - name=" + req.getName() + " type=" + req.getType());
        PokemonResponse.Builder response = PokemonResponse.newBuilder();

        // check for missing name
        if (req.getName().isEmpty()) {
            responseObserver.onNext(response.setIsSuccess(false).setError("name is required").build());
            responseObserver.onCompleted();
            return;
        }
        // check for valid type
        if (req.getType() == PokemonType.UNKNOWN || req.getType() == PokemonType.UNRECOGNIZED) {
            responseObserver.onNext(response.setIsSuccess(false).setError("type must be FIRE, WATER, or GRASS").build());
            responseObserver.onCompleted();
            return;
        }
        // check for duplicate name
        if (findByName(req.getName()) != null) {
            responseObserver.onNext(response.setIsSuccess(false)
                .setError("a Pokemon named '" + req.getName() + "' already exists").build());
            responseObserver.onCompleted();
            return;
        }

        // create the Pokemon and persist it
        JSONObject obj = new JSONObject();
        obj.put("id", nextId);
        obj.put("name", req.getName());
        obj.put("type", req.getType().name());
        pokemons.add(obj);
        savePokemon();

        Pokemon created = Pokemon.newBuilder()
            .setId(nextId)
            .setName(req.getName())
            .setType(req.getType())
            .build();
        nextId++;

        responseObserver.onNext(response.setIsSuccess(true).setPokemon(created).build());
        responseObserver.onCompleted();
    }

    @Override
    public synchronized void battlePokemon(BattlePokemonRequest req, StreamObserver<BattleResult> responseObserver) {
        System.out.println("Received: battlePokemon - p1=" + req.getPokemon1Name() + " p2=" + req.getPokemon2Name());
        BattleResult.Builder response = BattleResult.newBuilder();

        // check for missing names
        if (req.getPokemon1Name().isEmpty() || req.getPokemon2Name().isEmpty()) {
            responseObserver.onNext(response.setIsSuccess(false).setError("both pokemon names are required").build());
            responseObserver.onCompleted();
            return;
        }
        // check a pokemon cant battle itself
        if (req.getPokemon1Name().equalsIgnoreCase(req.getPokemon2Name())) {
            responseObserver.onNext(response.setIsSuccess(false).setError("a pokemon cannot battle itself").build());
            responseObserver.onCompleted();
            return;
        }

        JSONObject p1obj = findByName(req.getPokemon1Name());
        JSONObject p2obj = findByName(req.getPokemon2Name());

        if (p1obj == null) {
            responseObserver.onNext(response.setIsSuccess(false)
                .setError("pokemon '" + req.getPokemon1Name() + "' not found").build());
            responseObserver.onCompleted();
            return;
        }
        if (p2obj == null) {
            responseObserver.onNext(response.setIsSuccess(false)
                .setError("pokemon '" + req.getPokemon2Name() + "' not found").build());
            responseObserver.onCompleted();
            return;
        }

        Pokemon p1 = jsonToPokemon(p1obj);
        Pokemon p2 = jsonToPokemon(p2obj);
        String result = determineResult(p1.getType(), p2.getType());

        // determine winner name without ternary
        String winnerName;
        if (result.equals("TIE")) {
            winnerName = "none (tie)";
        } else if (result.equals("WIN")) {
            winnerName = p1.getName();
        } else {
            winnerName = p2.getName();
        }

        // record battle and persist
        JSONObject record = new JSONObject();
        record.put("p1_name", p1.getName());
        record.put("p1_type", p1.getType().name());
        record.put("p2_name", p2.getName());
        record.put("p2_type", p2.getType().name());
        record.put("result", result);
        record.put("winner_name", winnerName);
        battleHistory.add(record);
        saveHistory();

        responseObserver.onNext(response
            .setIsSuccess(true)
            .setPokemon1(p1)
            .setPokemon2(p2)
            .setResult(result)
            .setWinnerName(winnerName)
            .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listPokemon(com.google.protobuf.Empty req, StreamObserver<PokemonListResponse> responseObserver) {
        System.out.println("Received: listPokemon");
        PokemonListResponse.Builder response = PokemonListResponse.newBuilder();

        if (pokemons.isEmpty()) {
            responseObserver.onNext(response.setIsSuccess(false).setError("no pokemon have been created yet").build());
            responseObserver.onCompleted();
            return;
        }

        for (JSONObject p : pokemons) {
            response.addPokemons(jsonToPokemon(p));
        }

        responseObserver.onNext(response.setIsSuccess(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getBattleHistory(HistoryRequest req, StreamObserver<BattleHistoryResponse> responseObserver) {
        System.out.println("Received: getBattleHistory - limit=" + req.getLimit());
        BattleHistoryResponse.Builder response = BattleHistoryResponse.newBuilder();

        if (battleHistory.isEmpty()) {
            responseObserver.onNext(response.setIsSuccess(false).setError("no battles have been fought yet").build());
            responseObserver.onCompleted();
            return;
        }

        // return the most recent N battles
        int limit;
        if (req.getLimit() > 0) {
            limit = req.getLimit();
        } else {
            limit = battleHistory.size();
        }
        int start = Math.max(0, battleHistory.size() - limit);

        for (int i = start; i < battleHistory.size(); i++) {
            JSONObject b = battleHistory.get(i);

            PokemonType t1;
            try {
                t1 = PokemonType.valueOf(b.optString("p1_type", "UNKNOWN"));
            } catch (Exception e) {
                t1 = PokemonType.UNKNOWN;
            }

            PokemonType t2;
            try {
                t2 = PokemonType.valueOf(b.optString("p2_type", "UNKNOWN"));
            } catch (Exception e) {
                t2 = PokemonType.UNKNOWN;
            }

            BattleResult entry = BattleResult.newBuilder()
                .setIsSuccess(true)
                .setPokemon1(Pokemon.newBuilder().setName(b.optString("p1_name")).setType(t1).build())
                .setPokemon2(Pokemon.newBuilder().setName(b.optString("p2_name")).setType(t2).build())
                .setResult(b.optString("result"))
                .setWinnerName(b.optString("winner_name"))
                .build();
            response.addBattles(entry);
        }

        responseObserver.onNext(response.setIsSuccess(true).build());
        responseObserver.onCompleted();
    }
}
