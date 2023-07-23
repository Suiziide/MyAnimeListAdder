package org.suiz.MyAnimeListAdder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static String clientId = null;
    private static Map<String,String> tokenMap;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        String searchQuery = "";
        List<Result> topList = null;

        init();

        while (!searchQuery.equals("-1")) {
            int choice = 0;
            System.out.print("\nType in anime to search for (-1 to quit): ");

            searchQuery = in.nextLine().trim();
            if (!searchQuery.equals("-1")) {
                try {
                    topList = getSortedList(searchMAL(searchQuery));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }


                System.out.println("The top 10 results are:");
                for (int i = 0; i < 10; i++) {
                    String title = topList.get(i).getAlternative_titles().get("en");
                    if (title.isEmpty())
                        title = topList.get(i).getTitle();
                    System.out.println(i + 1 + ": " + title);
                }

                while(choice != -1){
                    System.out.println("Select a number to add to MAL (-1 to cancel)");
                    choice = in.nextInt();
                    if (choice != -1)
                        addAnime(topList.get(choice - 1).getId());
                    choice = -1;
                    in.nextLine();
                }
            }
        }
    }

    private static void addAnime(int id) {
        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("status", "plan_to_watch")
                .build();

        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + tokenMap.get("access_token"))
                .url("https://api.myanimelist.net/v2/anime/" + id + "/my_list_status")
                .patch(formBody)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful())
                System.out.println("Anime added successfully!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void init() {
        File tokenFile = new File(System.getProperty("user.home") + "\\MAL\\token.json");
        clientId = getClientId();
        if (tokenFile.isFile()) {
            try {
                tokenToMap(Files.readString(tokenFile.toPath()));
            } catch (IOException e) {
                tokenFile.delete();
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("No token file found - Starting authentication process...");
            authenticator();
        }
        refreshToken();

        System.out.println(" ------------------------------------------------------- ");
        System.out.println("|                                                       |");
        System.out.println("|                   ANIME SEARCH V1.10                  |");
        System.out.println("|                                                       |");
        System.out.println(" ------------------------------------------------------- ");
    }

    private static String getClientId() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://suiz.org/api/mal?client=MALadder")
                .header("Client", "MALadder")
                .build();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(client.newCall(request).execute().body().string())
            .get("clientId")
            .asText();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Takes a json encoded collection and processes it into a list of Result objects, each corrosponding to a
     * search result, then sorts it by popularity and returns the list
     * @param searchResult A json encoded collection of search results
     * @return A sorted List containing Result objects
     * @throws JsonProcessingException Might throw an exception if there's an error in processing the json
     */
    private static List<Result> getSortedList(String searchResult) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);

        List<Result> resultList = objectMapper.readValue(searchResult, new TypeReference<>() {
        });

        return resultList.stream().sorted(Comparator.comparingInt(Result::getPopularity)).toList();
    }

    /**
     * Uses the API on MAL to search for a given string and returns a json encoded collection with the results
     * @param anime the anime to search for
     * @return json encoded collection containing id, title, picture links, alternative titles and popularity of the results
     */
    private static String searchMAL(String anime){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.myanimelist.net/v2/anime?q=" + anime.replaceAll(" ", "%20") + "&limit=10&fields=id,title,main_picture,alternative_titles,popularity&nsfw=true")
                .header("X-MAL-CLIENT-ID", clientId)
                .addHeader("Content-Type","application/x-www-form-urlencoded")
                .build();
        try {
            return client.newCall(request).execute().body().string()
                    .replaceAll("(\"synonyms\":.*?],)|(\\{\"data\":)|(,\"paging.*)|(\\{\"node\":)", "")
                    .replaceAll("}}","}");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String codeGenerator(){
        final char[] allAllowed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~".toCharArray();

        //Use cryptographically secure random number generator
        SecureRandom random = new SecureRandom();

        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 128; i++) {
            code.append(allAllowed[random.nextInt(allAllowed.length)]);
        }

        return code.toString();
    }

    private static void authenticator(){
        boolean valid = false;
        Scanner input = new Scanner(System.in);
        String code_verifier = codeGenerator();
        String code_challenge = code_verifier;
        String code = "";
        System.out.println("\nGo to the following URL to authorize the application: \n" +
                "https://myanimelist.net/v1/oauth2/authorize?response_type=code&client_id=" +
                clientId + "&code_challenge=" + code_challenge + "&state=RequestID");
        while(!valid) {
            System.out.println("\nPaste in the URL you are redirected to here:");
            code = input.nextLine();
            Matcher regex = Pattern.compile("code=(.*)&state=RequestID").matcher(code);
            if (regex.find()) {
                valid = true;
                code = regex.group(1);
            } else {
                System.out.print("Not valid, please try again.");
            }
        }
        getToken(code, code_verifier);
    }

    private static void getToken(String code, String code_verifier){
        OkHttpClient client = new OkHttpClient();

        RequestBody form = new FormBody.Builder()
                .add("client_id",clientId)
                .add("code", code)
                .add("code_verifier", code_verifier)
                .add("grant_type", "authorization_code")
                .build();

        Request request = new Request.Builder()
                .url("https://myanimelist.net/v1/oauth2/token")
                .post(form)
                .build();

        try {
            Response response = client.newCall(request).execute();
            String body = response.body().string();

            saveTokenToFile(body);

            tokenToMap(body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void refreshToken() {
        OkHttpClient client = new OkHttpClient();

        RequestBody form = new FormBody.Builder()
                .add("client_id",clientId)
                .add("grant_type", "refresh_token")
                .add("refresh_token", tokenMap.get("refresh_token"))
                .build();

        Request request = new Request.Builder()
                .url("https://myanimelist.net/v1/oauth2/token")
                .post(form)
                .build();

        try {
            Response response = client.newCall(request).execute();
            String body = response.body().string();
            saveTokenToFile(body);

            tokenToMap(body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void saveTokenToFile(String body) throws IOException {
        File dir = new File(System.getProperty("user.home") + "\\MAL");
        if (!dir.exists()){
            dir.mkdirs();
        }

        BufferedWriter br = new BufferedWriter(new FileWriter(System.getProperty("user.home") + "\\MAL\\token.json", false));
        br.write(body);
        br.close();
    }

    private static void tokenToMap(String token) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, String>> typeRef = new TypeReference<>() {};
        tokenMap = mapper.readValue(token, typeRef);
    }
}