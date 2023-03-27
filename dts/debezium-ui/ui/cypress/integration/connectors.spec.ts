/// <reference types="cypress" />
describe("Connectors page", () => {
  before(() => {
    cy.visit("/");
  });
  it("should appear header logo", () => {
    cy.get(".pf-c-page__header")
      .find("img")
      .should("have.attr", "src")
      .should("include", "debezium_logo_300px");
  });
  it("Wait and check if able to connect backend and get connectors", () => {
    cy.intercept("/api/connectors/1").as("getConnector");
    cy.wait("@getConnector", { timeout: 50000 })
      .its("response.statusCode")
      .should("eq", 200);
  });
  it("Find create connector button and click", () => {
    cy.findByText("Create a connector", { timeout: 50000 }).click();
  });
  it("Selects connector type PostgreSQL and click next button", () => {
    cy.findByText("PostgreSQL database", { timeout: 50000 }).click();
    cy.findByRole("button", { name: "Next" }).click();
  });
  it("Fill the form to configure connector", () => {
    cy.get('input[name="connector&name"]', { timeout: 50000 }).type(
      "dbz-pg-conn"
    );
    cy.get('input[name="topic&prefix"]', { timeout: 50000 }).type(
      "fulfillment"
    );
    cy.get('input[name="database&hostname"]', { timeout: 50000 }).type(
      "dbzui-postgres"
    );
    cy.get('input[name="database&user"]', { timeout: 50000 }).type("postgres");
    cy.get('input[name="database&password"]', { timeout: 50000 }).type(
      "postgres"
    );
    cy.get('input[name="database&dbname"]', { timeout: 50000 }).type(
      "postgres"
    );
    cy.findByRole("button", { name: "Validate" }).click();
  });
  it("Checks if the form is valid", () => {
    cy.findByText("The validation was successful.").should("exist");
  });
  it("Skips to review step", () => {
    cy.findByRole("button", { name: "Review and finish" }).click();
  });
  it("Finish to configure connector", () => {
    cy.findByRole("button", { name: "Finish" }).click();
  });
  it("Connector action", () => {
    cy.get("tbody>tr", { timeout: 50000 })
      .eq(0)
      .find("td")
      .eq(5)
      .find(".pf-c-dropdown__toggle.pf-m-plain")
      .click();
  });
  it("Delete connector", () => {
    cy.get("ul>li").eq(5).scrollIntoView().findByText("Delete").click();
  });
  it("Confirm delete connector", () => {
    cy.get("body", { timeout: 50000 })
      .find(".pf-c-backdrop")
      .findByRole("button", { name: "Delete" })
      .click();
  });
});
