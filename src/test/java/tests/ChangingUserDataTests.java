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



public class ChangingUserDataTests {

    User user;
    String accessToken;

    @BeforeEach
    public void setUp() {
        // повторяющуюся для разных ручек часть URL запиcываем в переменную в BeforeEach
        RestAssured.baseURI = "https://stellarburgers.education-services.ru";
        user = new User("8kotik8@mail.ru", "8kotik8", "8kotik8");
        Response response = createNewUser(user);
        accessToken = response.then().extract().path("accessToken");
    }


    @Test
    @Description("Тест для проверки statusCode при обновлении UserName")
    public void checkStatusCode_changingUserName() {
        Response response = changeInformationAboutUser("8kotik8@mail.ru", "8kotik8", "changed");
        response.then().assertThat().statusCode(200);
    }


    @Test
    @Description("Тест для проверки ResponseBody при обновлении UserName")
    public void checkResponseBody_changingUserName() {
        Response response = changeInformationAboutUser("8kotik8@mail.ru", "8kotik8", "changed");
        response.then().assertThat()
                .body("success", equalTo(true))
                .body("user.email", equalTo(user.getEmail()))
                .body("user.name", equalTo("changed"));
    }

    // дополнительная проверка, важно не только получить новое имя в ответе на вызов метода .patch("/api/auth/user")
    // но и отдельно вызвать метод Получение информации о пользователе .get("/api/auth/user") и проверить что в системе name изменен
    @Test
    @Description("Тест для проверки что UserName изменен")
    public void check_changingUserName() {
        changeInformationAboutUser("8kotik8@mail.ru", "8kotik8", "changed");
        Response response = getInformationAboutUser();
        response.then().assertThat()
                .body("user.name", equalTo("changed"));
    }


    @Test
    @Description("Тест для проверки statusCode при обновлении email")
    public void checkStatusCode_changingUserEmail() {
        Response response = changeInformationAboutUser("8changed8@mail.ru", "8kotik8", "8kotik8");
        response.then().assertThat().statusCode(200);
    }


    @Test
    @Description("Тест для проверки ResponseBody при обновлении email")
    public void checkResponseBody_changingUserEmail() {
        Response response = changeInformationAboutUser("8changed8@mail.ru", "8kotik8", "8kotik8");
        response.then().assertThat()
                .body("success", equalTo(true))
                .body("user.email", equalTo("8changed8@mail.ru"))
                .body("user.name", equalTo(user.getName()));
    }

    // дополнительная проверка, важно не только получить новый email в ответе на вызов метода .patch("/api/auth/user")
    // но и отдельно вызвать метод Получение информации о пользователе .get("/api/auth/user") и проверить что в системе email изменен
    @Test
    @Description("Тест для проверки что email изменен")
    public void check_changingUserEmail() {
        changeInformationAboutUser("8changed8@mail.ru", "8kotik8", "8kotik8");
        Response response = getInformationAboutUser();
        response.then().assertThat()
                .body("user.email", equalTo("8changed8@mail.ru"));
    }


    @Test
    @Description("Тест для проверки statusCode и ResponseBody при попытке обновления email, на тот, который уже используется")
    public void check_changingUserEmailForDuplicate() {
        //создаем пользователя с email 8changed8@mail.ru
        User user2 = new User("8changed8@mail.ru", "8kotik8", "8kotik8");
        Response response2 = createNewUser(user2);
        // созраняем accessToken user2 в переменную чтобы после теста удалить его, так как в AfterEach мы удалить только одного пользователя из BeforeEach
        String user2Token = response2.then().extract().path("accessToken");

        // Созданному в BeforeEach пользователю пытаемся изменить email на 8changed8@mail.ru это почта user2
        Response response = changeInformationAboutUser("8changed8@mail.ru", "8kotik8", "8kotik8");
        response.then().assertThat().statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("User with such email already exists"));

        // теперь нам нужно удалить user2 с его токеном
        given()
                .header("Content-type", "application/json")
                .header("Authorization", user2Token)
                .delete("/api/auth/user");
    }


    @Test
    @Description("Тест для проверки statusCode при обновлении password")
    public void checkStatusCode_changingUserPassword() {
        Response response = changeInformationAboutUser("8kotik8@mail.ru", "changed", "8kotik8");
        response.then().assertThat().statusCode(200);
    }


    @Test
    @Description("Тест для проверки ResponseBody при обновлении password")
    public void checkResponseBody_changingUserPassword() {
        Response response = changeInformationAboutUser("8kotik8@mail.ru", "changed", "8kotik8");
        response.then().assertThat()
                .body("success", equalTo(true))
                .body("user.email", equalTo(user.getEmail()))
                .body("user.name", equalTo(user.getName()));
    }

    //Проверить пароль в ответе мы не можем, поэтому добавляем проверку что можно зарегистрироваться с новым/измененным паролем
    @Test
    @Description("Тест для проверки что изменен password")
    public void check_changingUserPassword() {
        changeInformationAboutUser("8kotik8@mail.ru", "changed", "8kotik8");
        Response response = loginUser("8kotik8@mail.ru", "changed");
        response.then().assertThat().statusCode(200);
    }


    // попробуем обновить пользователю данные на пустые значения
    // (в спеке не нашла описание, что в этом случае должно быть, опираемся на логику)
    @Test
    @Description("Тест для проверки statusCode при обновлении UserName")
    public void check_changingUserDateForEmptyDate() {
        Response response = changeInformationAboutUser("", "", "");
        response.then().assertThat().statusCode(403);
    }


    // Проверяем обновление без авторизации(не передаем токен или передаем не верный)
    @ParameterizedTest
    @CsvSource({
            "8kotik8@mail.ru, 8kotik8, changed, ''",
            "8kotik8@mail.ru, changed, 8kotik8, changed",
            "8changed8@mail.ru, kotik8, kotik8, Bearer 78747648",
            "8changed8@mail.ru, changed, changed, Bearer changed83743647364"
    })
    @Description("Тест для проверки обновления без авторизации")
    public void check_changingUserDateWithoutAuthorization(String email, String password, String name, String accessToken) {
        // Создаём Map для хранения email, password и name и добавляем их в мапу а ее в body
        Map <String, String> changingData = new HashMap<>();
        changingData.put("email", email);
        changingData.put("password", password);
        changingData.put("name", name);
        Response response = given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken)
                .body(changingData)
                .patch("/api/auth/user");

        response.then().assertThat().statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
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
    public void deleteUser(){
        given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken)
                .delete("/api/auth/user");
    }



    // метод для шага "Получение информации о пользователе"
    @Step("Send GET request to /api/auth/user")
    public Response getInformationAboutUser(){
        Response response = given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken)
                .get("/api/auth/user");
        return response;

    }

    // метод для шага "Изменение данных пользователя"
    @Step("Send PATCH request to /api/auth/user")
    public Response changeInformationAboutUser(String email, String password, String name){
        // Создаём Map для хранения email, password и name и добавляем их в мапу а ее в body
        Map <String, String> changingData = new HashMap<>();
        changingData.put("email", email);
        changingData.put("password", password);
        changingData.put("name", name);
        Response response = given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken)
                .body(changingData)
                .patch("/api/auth/user");
        return response;

    }


    // Удаляем созданного пользователя после каждого теста
    @AfterEach
    public void deleteUserAfterEach(){
        deleteUser();
    }

}
