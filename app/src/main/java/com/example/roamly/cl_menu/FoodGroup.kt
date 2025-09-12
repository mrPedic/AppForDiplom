package com.example.roamly.cl_menu

class FoodGroup{
    private var foodGroup = mutableListOf<Food>()

    public fun setFoodMenu (food: FoodGroup){
        this.foodGroup = food.foodGroup
    }
    public fun addFoodToMenu(food: Food){
        this.foodGroup.add(food)
    }
    public fun getFoodMenu(): MutableList<Food>{
        return this.foodGroup
    }
}