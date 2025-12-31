    graph TD
    %% --- Styles ---
    classDef frontend fill:#ADD8E6,stroke:#333,stroke-width:1px;
    classDef backend fill:#90EE90,stroke:#333,stroke-width:1px;
    classDef db fill:#FFFFE0,stroke:#333,stroke-width:1px;
    classDef infra fill:#D3D3D3,stroke:#333,stroke-width:1px;
    classDef extension fill:#E6E6FA,stroke:#333,stroke-width:1px;
    classDef noteCls fill:#FFF,stroke:#333,stroke-dasharray: 5 5,text-align:left;

    %% --- Client Layer ---
    subgraph Client_Layer [Client Layer]
        Browser["Web Browser<br>(Admin Dashboard / Student Portal)"]
        Mobile["Mobile Apps<br>(WhatsApp / Telegram)"]
    end

    %% --- Infrastructure Layer ---
    subgraph Infra_Layer [Infrastructure Layer (AWS)]
        direction TB
        Cloudflare["Cloudflare<br>(CDN & WAF)"]:::infra
        Nginx["Nginx Gateway<br>(Reverse Proxy & SSL)"]:::infra
    end

    %% --- Application Layer ---
    subgraph App_Layer [Application Layer]
        direction TB
        
        Frontend["Frontend Client<br>(React SPA)"]:::frontend
        
        %% Nested Backend Components
        subgraph Backend_Wrapper [Core Backend Service (Spring Boot)]
            direction TB
            WebLayer["Web Layer<br>(Security / Controllers)"]:::backend
            ServiceLayer["Service Layer<br>(Business Logic / Validation)"]:::backend
            Timefold["Optimization Engine<br>(Timefold Solver)"]:::backend
            DAL["Data Access Layer<br>(Hibernate / JPA)"]:::backend
        end

        AIService["AI Integration Service<br>(The Connector)"]:::extension
    end

    %% --- Data Layer ---
    subgraph Data_Layer [Data Layer]
        DB[("MySQL Database<br>(RDS / EC2)")]:::db
    end

    %% --- Notes (Visualized as nodes connected by dotted lines) ---
    NoteTimefold["<b>Core Innovation:</b><br>Calculates Schedule using<br>Constraint Streams<br>(Dual-Mode Logic)"]:::noteCls
    NoteAI["<b>Planned Extension:</b><br>Translates natural language<br>to API calls via<br>Model Context Protocol (MCP)"]:::noteCls

    %% --- Relationships ---

    %% Client to Infra
    Browser -->|"HTTPS / WSS"| Cloudflare
    Mobile -->|"HTTPS"| Cloudflare

    %% Infra Routing
    Cloudflare -->|"Filtered Traffic"| Nginx
    Nginx -->|"Serve Static Files"| Frontend
    Nginx -->|"Proxy API Requests"| WebLayer
    Nginx -->|"Route Messaging Events"| AIService

    %% App Logic
    Frontend -->|"JSON / REST Calls"| WebLayer
    AIService <-->|"JSON-RPC (Structure Commands)"| WebLayer

    %% Backend Internals
    WebLayer --> ServiceLayer
    ServiceLayer --> DAL
    ServiceLayer <-->|"Start/Stop Solving"| Timefold

    %% Data Access
    DAL -->|"JDBC / SQL"| DB

    %% Attach Notes
    Timefold -.- NoteTimefold
    AIService -.- NoteAI