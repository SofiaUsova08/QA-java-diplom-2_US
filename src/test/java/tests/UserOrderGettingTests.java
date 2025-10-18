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
import java.util.List;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

public class UserOrderGettingTests {

    List<String> ingredients = new ArrayList<>();
    Order order;

    User user = new User("sof0808@mail.ru", "sof0808", "sof0808");;
    String accessToken;


    @BeforeEach
    public void setUp() {
        // повторяющуюся для разных ручек часть URL запиcываем в переменную в BeforeEach
        RestAssured.baseURI = "https://stellarburgers.education-services.ru";
        ingredients.add("61c0c5a71d1f82001bdaaa70");
        order = new Order(ingredients);
        createNewOrderWitUserAuthorization(order);
    }


    @Test
    @Description("Тест для проверки statusCode при Получении заказов конкретного пользователя с авторизацией")
    public void checkStatusCode_getOrdersWithUserAuthorization() {
        Response response = getOrdersWithUserAuthorization();
        response.then().assertThat().statusCode(200);
    }


    //Важно проверить что переданный при заказе ингридиент содержится в ответе на запрос Получения заказов пользователя
    @Test
    @Description("Тест для проверки ResponseBody при Получении заказов конкретного пользователя с авторизацией")
    public void checkResponseBody_getOrdersWithUserAuthorization() {
        Response response = getOrdersWithUserAuthorization();
        response.then().assertThat()
                .body("success", equalTo(true))
                .body("orders[0].ingredients", hasItem("61c0c5a71d1f82001bdaaa70"));
    }


    @Test
    @Description("Тест для проверки statusCode при Получении заказов конкретного пользователя без авторизации")
    public void checkStatusCode_getOrdersWithouthUserAuthorization() {
        Response response = getOrdersWithouthUserAuthorization();
        response.then().assertThat().statusCode(401);
    }


    @Test
    @Description("Тест для проверки ResponseBody при Получении заказов конкретного пользователя без авторизации")
    public void checkResponseBody_getOrdersWithouthUserAuthorization() {
        Response response = getOrdersWithouthUserAuthorization();
        response.then().assertThat()
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }



    // метод для шага "Получение заказов конкретного пользователя c авторизацией"
    @Step("Send POST request to /api/orders")
    public Response getOrdersWithUserAuthorization(){
        Response response = given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken)
                .get("/api/orders");
        return response;
    }

    // метод для шага "Получение заказов конкретного пользователя без авторизации"
    @Step("Send POST request to /api/orders")
    public Response getOrdersWithouthUserAuthorization(){
        Response response = given()
                .header("Content-type", "application/json")
                .get("/api/orders");
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

    // метод для шага "Удаление пользователя"
    @Step("Send DELETE request to /api/auth/user")
    public void deleteUser(){
        given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken)
                .delete("/api/auth/user");
    }

    // Удаляем созданного пользователя после каждого теста
    @AfterEach
    public void deleteUserAfterEach() {
        deleteUser();
    }


}