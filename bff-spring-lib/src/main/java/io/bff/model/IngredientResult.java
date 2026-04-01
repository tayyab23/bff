package io.bff.model;

public class IngredientResult {
    public int status;
    public Object body;

    public IngredientResult(int status, Object body) {
        this.status = status;
        this.body = body;
    }
}
