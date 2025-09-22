package tests;

import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import jdk.jfr.Description;
import models.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.HashMap;
import java.util.Map;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;


public class UserCreatingTests {

    User user = new User("8kotik8@mail.ru", "8kotik8", "8kotik8");

    @BeforeEach
    public void setUp() {
        // повторяющуюся для разных ручек часть URL запиcываем в переменную в BeforeEach
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
    }

    @Test
    @Description("Тест для проверки statusCode при создании нового пользователя")
    public void checkStatusCode_NewUserCreation() {
        Response response = createNewUser(user);
        response.then().assertThat().statusCode(200);
    }

    @Test
    @Description("Проверяем тело ответа при создании нового пользователя") // намерено не стала дробить на 5 отдельных тестов! (но если критично могу разделить)
    public void checkResponseBody_NewUserCreation() {
        Response response = createNewUser(user);
        response.then().assertThat()
                .body("success", equalTo(true))
                .body("user.email", equalTo(user.getEmail()))
                .body("user.name", equalTo(user.getName()))
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }


    @Test
    @Description("Тест для проверки statusCode при попытки создать дубликат пользователя")
    public void checkStatusCode_CannotCreateDuplicateUser() {
        Response response = createNewUser(user);
        Response responseDuplicate = createNewUser(user);
        responseDuplicate.then().assertThat().statusCode(403);

    }

    @Test
    @Description("Тест для проверки ResponseBody при попытки создать дубликат пользователя")
    public void checkResponseBody_CannotCreateDuplicateUser() {
        Response response = createNewUser(user);
        Response responseDuplicate = createNewUser(user);
        responseDuplicate.then().assertThat()
                .body("success", equalTo(false))
                .body("message", equalTo("User already exists"));
    }


    @ParameterizedTest
    @CsvSource({
            "8kotik8@mail.ru, 8kotik8, ''",
            "8kotik8@mail.ru, '', 8kotik8",
            "'', 8kotik8, 8kotik8"
    })
    @Description("Тест для проверки statusCode при создании нового пользователя без одного из обязательных полей,")
    public void checkStatusCode_NewUserCreationWithoutRequiredFields(String email, String password, String name) {
        User userParameterized = new User(email, password, name);
        Response response = createNewUser(userParameterized);
        response.then().assertThat().statusCode(403);
    }

    @ParameterizedTest
    @CsvSource({
            "8kotik8@mail.ru, 8kotik8, ''",
            "8kotik8@mail.ru, '', 8kotik8",
            "'', 8kotik8, 8kotik8"
    })
    @Description("Тест для проверки ResponseBody при создании нового пользователя без одного из обязательных полей,")
    public void checkResponseBody_NewUserCreationWithoutRequiredFields(String email, String password, String name) {
        User userParameterized = new User(email, password, name);
        Response response = createNewUser(userParameterized);
        response.then().assertThat()
                .body("success", equalTo(false))
                .body("message", equalTo("Email, password and name are required fields"));
    }



    // метод для шага "Создание пользователя"
    @Step("Send POST request to /api/auth/register")
    public Response createNewUser(User user){
        Response response = given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/register");
        return response;
    }

    // метод для шага "Логин пользователя в системе"
    @Step("Send POST request to /api/auth/login")
    public Response loginUser(User user){
        // Геттерами получаем email и password пользователя и сохраняем в переменные
        String email = user.getEmail();
        String password = user.getPassword();
        // Создаём Map для хранения email и password и добавляем их в мапу а ее в body
        Map <String, String> authorizationData = new HashMap<>();
        authorizationData.put("email", email);
        authorizationData.put("password", password);

        Response response = given()
                .header("Content-type", "application/json")
                .body(authorizationData)
                .post("/api/auth/login");
        return response;
    }


    // метод для шага "Удаление пользователя"
    @Step("Send DELETE request to /api/auth/user")
    public void deleteUser(User user){
        //Сначала вызываем loginUser чтобы получить accessToken, нужен для удаления
        Response loginResponse = loginUser(user);
        // Проверяем, успешен ли был логин, если да, удаляем пользователя
        if (loginResponse.then().extract().statusCode() == 200) {
            String accessToken = loginResponse.then().extract().path("accessToken");
            given()
                    .header("Content-type", "application/json")
                    .header("Authorization", accessToken)
                    .delete("/api/auth/user");
        }
        // Если логин не успешен, ничего не делаем
    }

    // Удаляем созданного пользователя после каждого теста
    @AfterEach
    public void deleteUserAfterEach() {
        deleteUser(user);
    }

}
