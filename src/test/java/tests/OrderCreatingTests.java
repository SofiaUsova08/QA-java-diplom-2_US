package tests;

import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import jdk.jfr.Description;
import models.Order;
import models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderCreatingTests {

    List<String> ingredients = new ArrayList<>();
    Order order;

    User user = new User("sof0808@mail.ru", "sof0808", "sof0808");;
    String accessToken;


    @BeforeEach
    public void setUp() {
        // повторяющуюся для разных ручек часть URL запиcываем в переменную в BeforeEach
        RestAssured.baseURI = "https://stellarburgers.education-services.ru";
        order = new Order(ingredients);
    }


    @Test
    @Description("Тест для проверки statusCode при создании нового заказа без авторизации и с ингридиентами")
    public void checkStatusCode_NewOrderCreationWithoutUserAuthorizationAndWithIngredients() {
        ingredients.add("61c0c5a71d1f82001bdaaa70");
        Response response = createNewOrderWithoutUserAuthorization(order);
        response.then().assertThat().statusCode(200);
    }

    @Test
    @Description("Тест для проверки ResponseBody при создании нового заказа без авторизации и с ингридиентами")
    public void checkResponseBody_NewOrderCreationWithoutUserAuthorizationAndWithIngredients() {
        ingredients.add("61c0c5a71d1f82001bdaaa70");
        Response response = createNewOrderWithoutUserAuthorization(order);
        response.then().assertThat()
                .body("success", equalTo(true))
                .body("order.number", isA(Integer.class))
                .body("name", not(emptyString()));
    }


    @Test
    @Description("Тест для проверки statusCode при создании нового заказа c авторизацией и с ингридиентами")
    public void checkStatusCode_NewOrderCreationWitUserAuthorizationAndWithIngredients() {
        ingredients.add("61c0c5a71d1f82001bdaaa70");
        Response response = createNewOrderWitUserAuthorization(order);
        response.then().assertThat().statusCode(200);
    }


    @Test
    @Description("Тест для проверки ResponseBody при создании нового заказа c авторизацией и с ингридиентами")
    public void checkResponseBody_NewOrderCreationWitUserAuthorizationAndWithIngredients() {
        ingredients.add("61c0c5a71d1f82001bdaaa70");
        Response response = createNewOrderWitUserAuthorization(order);
        response.then().assertThat()
                .body("success", equalTo(true))
                .body("order.number", isA(Integer.class))
                .body("name", not(emptyString()));
    }


    @Test
    @Description("Тест для проверки statusCode при создании нового заказа без авторизации и без ингридиентов")
    public void checkStatusCode_NewOrderCreationWithoutUserAuthorizationAndWithoutIngredients() {
        Response response = createNewOrderWithoutUserAuthorization(order);
        response.then().assertThat().statusCode(400);
    }

    @Test
    @Description("Тест для проверки ResponseBody при создании нового заказа c авторизацией и без ингридиентов")
    public void checkResponseBody_NewOrderCreationWitUserAuthorizationAndWithoutIngredients() {
        Response response = createNewOrderWitUserAuthorization(order);
        response.then().assertThat()
                .body("success", equalTo(false))
                .body("message", equalTo("Ingredient ids must be provided"));
    }


    @Test
    @Description("Тест для проверки statusCode при создании нового заказа без авторизации и c невалидным хешом ингредиента")
    public void checkStatusCode_NewOrderCreationWithoutUserAuthorizationAndWithInvalidHashIngredient() {
        ingredients.add("61c0c5a71d1f8200hfuehfu83493uwhduwhudjfwdh1bdaaa70");
        Response response = createNewOrderWithoutUserAuthorization(order);
        response.then().assertThat().statusCode(500);
    }

    @Test
    @Description("Тест для проверки ResponseBody при создании нового заказа c авторизацией и c невалидным хешом ингредиента")
    public void checkResponseBody_NewOrderCreationWitUserAuthorizationAndWithInvalidHashIngredient() {
        ingredients.add("61c0c5a71d1f8200hfuehfu83493uwhduwhudjfwdh1bdaaa70");
        Response response = createNewOrderWitUserAuthorization(order);
        response.then().assertThat()
                .body(containsString("Internal Server Error"));
    }



    // Важно проверить не только ответ на вызов метода /api/orders но и что заказ действительно создан
    @Test
    @Description("Тест для проверки что созданый заказ есть в системе")
    public void check_NewOrderCreationInSystem() {
        ingredients.add("61c0c5a71d1f82001bdaaa70");
        Response response = createNewOrderWitUserAuthorization(order);
        response.then().assertThat().statusCode(200);
        int numberAtCreation = response.then().extract().path("order.number");

        Response responseGetUserOrder = given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken)
                .get("/api/orders");
        int numberAtGetUserOrder = responseGetUserOrder.then().extract().path("orders[0].number");
        assertEquals(numberAtCreation, numberAtGetUserOrder);
    }



    // метод для шага "Создание заказа без авторизации пользовалеля"
    @Step("Send POST request to /api/orders")
    public Response createNewOrderWithoutUserAuthorization(Order order){
        Response response = given()
                .header("Content-type", "application/json")
                .body(order)
                .post("/api/orders");
        return response;
    }


    // метод для шага "Создание заказа с авторизацией пользовалеля"
    @Step("Send POST request to /api/orders")
    public Response createNewOrderWitUserAuthorization(Order order){
        Response  responseCreateNewUser = createNewUser(user);
        accessToken = responseCreateNewUser.then().extract().path("accessToken");
        Response response = given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken)
                .body(order)
                .post("/api/orders");
        return response;
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
        Map<String, String> authorizationData = new HashMap<>();
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
    public void deleteUserAfterEach(){
        deleteUser(user);
    }

}