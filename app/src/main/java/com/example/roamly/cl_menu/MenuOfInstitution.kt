package com.example.roamly.cl_menu

class MenuOfInstitution{
    private var foodMenu = mutableListOf<FoodGroup>()
    private var drinksMenu = mutableListOf<DrinksGroup>()

    public fun addFoodGroup(foodGroup: FoodGroup){
        this.foodMenu.add(foodGroup)
    }
    public fun addDrinksGroup(drinksGroup: DrinksGroup){
        this.drinksMenu.add(drinksGroup)
    }
}