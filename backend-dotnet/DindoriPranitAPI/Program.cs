using System.Text;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using DindoriPranitAPI.Services;
using Microsoft.AspNetCore.RateLimiting;
using System.Threading.RateLimiting;

var builder = WebApplication.CreateBuilder(args);

// Force the correct database connection string to bypass Somee's default connection string overrides
builder.Configuration["ConnectionStrings:DefaultConnection"] = "workstation id=DindoriPranitDb.mssql.somee.com;packet size=4096;user id=dindoriadmin_SQLLogin_1;pwd=98njtsyv8o;data source=tcp:DindoriPranitDb.mssql.somee.com,1433;persist security info=False;initial catalog=DindoriPranitDb;TrustServerCertificate=True;MultipleActiveResultSets=True";

// Add services to the container.
builder.Services.AddControllers();
builder.Services.AddSingleton<FcmService>();
builder.Services.AddSingleton<DindoriPranitAPI.Services.WhatsAppService>();
builder.Services.AddSingleton<DindoriPranitAPI.Services.ChatBotService>();
builder.Services.AddSingleton<DindoriPranitAPI.Services.WebhookService>();
builder.Services.AddSingleton<DindoriPranitAPI.Services.EmailService>();
builder.Services.AddHostedService<QueueProcessorService>();

builder.Services.AddRateLimiter(options =>
{
    options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
    options.AddFixedWindowLimiter("LoginPolicy", opt =>
    {
        opt.Window = TimeSpan.FromMinutes(1);
        opt.PermitLimit = 5; // Allow max 5 logins/registrations per minute
        opt.QueueLimit = 0;
    });
});

// Swagger configuration for API testing
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo { Title = "Dindori Pranit Yadnyiki API", Version = "v1" });
    
    // Add Bearer Token authorization to Swagger UI
    c.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Description = "JWT Authorization header using the Bearer scheme. Example: \"Authorization: Bearer {token}\"",
        Name = "Authorization",
        In = ParameterLocation.Header,
        Type = SecuritySchemeType.ApiKey,
        Scheme = "Bearer"
    });

    c.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference
                {
                    Type = ReferenceType.SecurityScheme,
                    Id = "Bearer"
                }
            },
            Array.Empty<string>()
        }
    });
});

// Configure JWT Authentication
var jwtSecret = builder.Configuration["Jwt:Secret"] ?? throw new InvalidOperationException("JWT Secret key is not configured.");
var key = Encoding.ASCII.GetBytes(jwtSecret);

builder.Services.AddAuthentication(options =>
{
    options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
})
.AddJwtBearer(options =>
{
    options.RequireHttpsMetadata = !builder.Environment.IsDevelopment();
    options.SaveToken = true;
    options.TokenValidationParameters = new TokenValidationParameters
    {
        ValidateIssuerSigningKey = true,
        IssuerSigningKey = new SymmetricSecurityKey(key),
        ValidateIssuer = !builder.Environment.IsDevelopment(),
        ValidateAudience = !builder.Environment.IsDevelopment(),
        ValidIssuer = builder.Configuration["Jwt:Issuer"] ?? "DindoriPranitAuthority",
        ValidAudience = builder.Configuration["Jwt:Audience"] ?? "DindoriPranitClients",
        ClockSkew = TimeSpan.Zero
    };
});

// Configure CORS for Mobile / Web Clients
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll",
        policy =>
        {
            var allowedOrigins = builder.Configuration.GetSection("Cors:AllowedOrigins").Get<string[]>() 
                ?? new[] { "http://localhost:3000", "https://localhost:3000", "http://dindoripranit.somee.com", "https://dindoripranit.somee.com" };
            policy.WithOrigins(allowedOrigins)
                  .AllowAnyMethod()
                  .AllowAnyHeader();
        });
});

var app = builder.Build();

// Run self-healing database migrations on startup
var connString = app.Configuration.GetConnectionString("DefaultConnection");
if (!string.IsNullOrEmpty(connString))
{
    DindoriPranitAPI.Services.DatabaseMigrator.Migrate(connString);
}

app.UseMiddleware<DindoriPranitAPI.Middleware.ExceptionHandlingMiddleware>();
app.UseMiddleware<DindoriPranitAPI.Middleware.AuditLoggingMiddleware>();
app.UseMiddleware<DindoriPranitAPI.Middleware.IdempotencyMiddleware>();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI(c =>
    {
        c.SwaggerEndpoint("/swagger/v1/swagger.json", "Dindori Pranit Yadnyiki API v1");
    });
}

app.UseHttpsRedirection();
app.UseStaticFiles();
app.UseCors("AllowAll");
app.UseRateLimiter();

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();
