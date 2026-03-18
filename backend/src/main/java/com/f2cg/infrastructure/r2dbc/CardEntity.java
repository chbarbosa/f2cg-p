package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("cards")
public class CardEntity {

    @Id
    private String id;
    private String name;
    @Column("mana_cost")
    private int manaCost;
    @Column("card_type")
    private String cardType;
    private String theme;
    @Column("unit_class")
    private String unitClass;
    private Integer attack;
    private Integer defense;
    @Column("effect_type")
    private String effectType;
    @Column("effect_value")
    private Integer effectValue;

    public CardEntity() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public int getManaCost() { return manaCost; }
    public String getCardType() { return cardType; }
    public String getTheme() { return theme; }
    public String getUnitClass() { return unitClass; }
    public Integer getAttack() { return attack; }
    public Integer getDefense() { return defense; }
    public String getEffectType() { return effectType; }
    public Integer getEffectValue() { return effectValue; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setManaCost(int manaCost) { this.manaCost = manaCost; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    public void setTheme(String theme) { this.theme = theme; }
    public void setUnitClass(String unitClass) { this.unitClass = unitClass; }
    public void setAttack(Integer attack) { this.attack = attack; }
    public void setDefense(Integer defense) { this.defense = defense; }
    public void setEffectType(String effectType) { this.effectType = effectType; }
    public void setEffectValue(Integer effectValue) { this.effectValue = effectValue; }
}