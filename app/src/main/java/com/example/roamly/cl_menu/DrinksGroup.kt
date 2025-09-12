package com.example.roamly.cl_menu

class DrinksGroup {
    var drinksGroup = mutableListOf<Drinks>()

    public fun setDrinksMenu(drinks: DrinksGroup){
        this.drinksGroup = drinks.drinksGroup
    }
    public fun addDrinkToMenu(drink: Drinks){
        this.drinksGroup.add(drink)
    }
    public fun getDrinksMenu(): MutableList<Drinks>{
        return this.drinksGroup
    }
}