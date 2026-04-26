FROM mcr.microsoft.com/dotnet/sdk:8.0-alpine AS build
WORKDIR /src

COPY dotnet-guardian.csproj .
RUN dotnet restore

COPY . .
RUN dotnet publish dotnet-guardian.csproj -c Release -o /app/publish /p:UseAppHost=false

FROM mcr.microsoft.com/dotnet/aspnet:8.0-alpine
WORKDIR /app

RUN apk add --no-cache wget \
    && addgroup -S app \
    && adduser -S app -G app

COPY --from=build /app/publish .

ENV ASPNETCORE_URLS=http://+:8080

EXPOSE 8080

USER app
ENTRYPOINT ["dotnet", "dotnet-guardian.dll"]
