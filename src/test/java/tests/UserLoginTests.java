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


public class UserLoginTests {

    User user = new User("8kotik8@mail.ru", "8kotik8", "8kotik8");

    @BeforeEach
    public void setUp() {
        // повторяющуюся для разных ручек часть URL запиcываем в переменную в BeforeEach
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
    }

    @Test
    @Description("Тест для проверки statusCode при успешном логине пользователя")
    public void checkStatusCode_UserLogin() {
        createNewUser(user);
        Response response = loginUser(user.getEmail(), user.getPassword());
        response.then().assertThat().statusCode(200);
    }

    @Test
    @Description("Тест для проверки ResponseBody при успешном логине пользователя")
    public void checkResponseBody_UserLogin() {
        createNewUser(user);
        Response response = loginUser(user.getEmail(), user.getPassword());
        response.then().assertThat()
                .body("success", equalTo(true))
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("user.email", equalTo(user.getEmail()))
                .body("user.name", equalTo(user.getName()));

    }

    @ParameterizedTest
    @CsvSource({
            "8kotik8@mail.ru, sdfjsdhfj",
            "dkmkljdk, 8kotik8",
            "lkihgy, jsfje7f",
            "'', 8kotik8",
            "8kotik8@mail.ru, ''",
            "'', ''"
    })
    @Description("Тест для проверки statusCode при логине пользователя, если логин или пароль неверные или нет одного из полей")
    public void checkStatusCode_UserLoginWithoutRequiredFieldsAndIncorrectRequiredFields(String email, String password) {
        createNewUser(user);
        Response response = loginUser(email, password);
        response.then().assertThat().statusCode(401);
    }


    @ParameterizedTest
    @CsvSource({
            "8kotik8@mail.ru, sdfjsdhfj",
            "dkmkljdk, 8kotik8",
            "lkihgy, jsfje7f",
            "'', 8kotik8",
            "8kotik8@mail.ru, ''",
            "'', ''"
    })
    @Description("Тест для проверки ResponseBody при логине пользователя, если логин или пароль неверные или нет одного из полей")
    public void checkResponseBody_UserLoginWithoutRequiredFieldsAndIncorrectRequiredFields(String email, String password) {
        createNewUser(user);
        Response response = loginUser(email, password);
        response.then().assertThat()
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));
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
    public Response loginUser(String email, String password){
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
        Response loginResponse = loginUser(user.getEmail(), user.getPassword());
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
