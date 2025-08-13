# frontend_sandbox_hps
##  Overview

This project automates the generation of API documentation and related assets from structured Excel files.  
It is designed for API teams to accelerate documentation creation and maintain consistency across technical deliverables.

**Key Deliverables:**
- **OpenAPI YAML files** (Request, Response, Aggregates)
- **JSON files** (module descriptor,nominal case)
- **SQL scripts** and **Java DTOs**
- **PowerPoint presentations** from a template
- **PNG diagrams/screenshots** from generated PPT slides

---

## Features

- **Excel-driven API specification parsing**
- **Automatic OpenAPI 3.0 schema generation**
- **SQL & Java code generation** for database types and DTOs
- **PPT generation** with placeholder replacement
- **PPT to PNG export** for visual documentation
- Supports **API filtering** and **debug logging**
- Customizable templates for YAML, PPT, and static assets
  
  ---

##  Requirements

- **Java 8** or higher
- [Apache POI](https://poi.apache.org/) – For processing Excel files
- [Jackson](https://github.com/FasterXML/jackson) – For JSON serialization
- **Maven 3.6+**  


  ---
## Author
Mouna Ed-daoudi , Douaa Elhaddoudi — Internship at HPS
