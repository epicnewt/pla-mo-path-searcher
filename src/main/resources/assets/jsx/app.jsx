// noinspection JSXNamespaceValidation,JSUnresolvedVariable

const {
    Accordion,
    AccordionSummary,
    AccordionDetails,
    AppBar,
    Box,
    Button,
    Checkbox,
    CircularProgress,
    FormControlLabel,
    FormControl,
    InputLabel,
    List,
    ListItem,
    ListItemText,
    MenuItem,
    Select,
    SvgIcon,
    Switch,
    TextField,
    Toolbar,
    Typography,
} = MaterialUI

const {
    useState,
    useEffect,
    useRef,
    useCallback
} = React

const queryParams = {};
location.search.substr(1)
    .split("&")
    .forEach((item) => {
        queryParams[item.split("=")[0]] = decodeURIComponent(item.split("=")[1])
    })

console.log(queryParams)

const pkmn = loadPokemon()[queryParams.species]

if (pkmn) {
    document.title = pkmn.name;
    queryParams.fixedGender = pkmn.fixedGender ? 'true' : 'false'
}

const Header = () => (
    <AppBar position="static">
        <Toolbar>
            <Typography variant="h6">
                Pokémon Arceus Legends Path Searcher
            </Typography>
        </Toolbar>
    </AppBar>
)
const Footer = () => (
    <section className='footer'>
        <Typography variant='subtitle'>
            Optimal path finder by @EpicNewt, based on RNG research by @Lincoln-LM in PLA-Live-Map
        </Typography>
    </section>
)

function toActionDescription(actions) {
    return actions.map(i => {
        if (i <= -1) {
            return `Battle ${i * -1}`
        } else if (i >= -1) {
            return `Catch ${Math.abs(i)}`
        }
    });
}

const PokemonListItem = ({sidtid, gender, nature, shiny, alpha, ivs}) => {
    const [strike, setStrike] = useState(1)
    const onClick = useCallback(() => {
        setStrike(strike + 1)
    })
    return (
        <ListItemText key={sidtid}
                      style={{textDecoration: strike % 2 ? undefined : 'line-through'}}
                      onClick={onClick}
                      secondary={
                          <div>
                              {genderSymbols[gender]}
                              {`${ivs.evs} ${nature} `}
                              {shiny && ' shiny'}
                              {alpha && ' alpha'}
                          </div>
                      }
        />
    );
}

const SearchResult = ({result, spawns}) => {
    const remainingSpawns = parseInt(queryParams["spawns"] || spawns) - (result.path.reduce((acc, n) => acc + Math.abs(n), 0) + 1);

    const lastIndex = result.advances.length - 1;
    return (
        <div>{
            result.advances.map((advance, i) => {
                const slice = i === lastIndex ? advance.reseeds : advance.reseeds.slice(0, -1);
                const hasMatch = slice.some(g => g.pokemon.some(({alpha, shiny}) => (alpha && shiny)))
                const desc = (hasMatch ? '[Shiny Alpha]' : '')
                return (
                    <Accordion key={i}>
                        <AccordionSummary
                            expandIcon={<SvgIcon>
                                <path d="m12 8-6 6 1.41 1.41L12 10.83l4.59 4.58L18 14z"/>
                            </SvgIcon>}
                            aria-controls="panel1a-content"
                            id="panel1a-header"
                        >
                            <Typography>Advance {i}: {[...toActionDescription(advance.actions), ...((result.advances.length - 1 === i) ? [] : ['Return to town'])].join(", ")} {desc}</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <List>
                                {advance.reseeds.map((reseed, i) => [
                                    <ListItem key={reseed.groupSeed}>
                                        <ListItemText
                                            primary={`Group seed: ${reseed.groupSeed}`}
                                        />
                                    </ListItem>,
                                    reseed.pokemon.map(p => (
                                        <ListItem key={p.seed}>
                                            {/*{`"seed": "C990BE9317900E1F","sidtid": 3032674752,"pid": 3554362865,"ivs": {"hp": 24,  "att": 20,  "def": 19,  "spAtt": 1,  "spDef": 12,  "speed": 15,  "evs": "110000"},"nature": "Brave","gender": 43,"shiny": false,"alpha": false}`}*/}
                                            <PokemonListItem {...p} />
                                        </ListItem>
                                    ))
                                ])}
                            </List>
                        </AccordionDetails>
                    </Accordion>
                )
            })
        }</div>
    )
}
const SearchResults = ({data, spawns}) => {
    if (!data || data.length === 0) {
        return <span>No results found with 10 actions or less. Search stopped.</span>
    }

    if (data.length === 1 && !queryParams['continue'])
        return <SearchResult result={data[0]} spawns={spawns}/>

    const pathPrefix = queryParams['continue'] ? queryParams['continue'] + " -> " : ""
    return data.map((result, i) => (
            <Accordion key={i}>
                <AccordionSummary
                    expandIcon={<SvgIcon>
                        <path d="m12 8-6 6 1.41 1.41L12 10.83l4.59 4.58L18 14z"/>
                    </SvgIcon>}
                    aria-controls="panel1a-content"
                    id="panel1a-header"
                >
                    <Typography>{pathPrefix + result.pathDescription}</Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <SearchResult result={result} spawns={spawns}/>
                </AccordionDetails>
            </Accordion>
        )
    )
}

const useLocalStorage = (key, defaultValue) => {
    const isBool = defaultValue === true || defaultValue === false;
    const restoredDefault = isBool
        ? (queryParams[key] || localStorage.getItem(key)) === 'true'
        : queryParams[key] || localStorage.getItem(key)
    const useStateArr = useState(restoredDefault || defaultValue);
    const [state, setState] = useStateArr

    const advancedSetter = useCallback((newValue) => {
        localStorage.setItem(key, newValue);
        setState(newValue)
    })
    return [state, advancedSetter]
}

const genderSymbols = {
    FEMALE: <span style={{color: "#F6814A", fontFamily: 'sans-serif'}}>♀</span>,
    MALE: <span style={{color: "#499FFF", fontFamily: 'sans-serif'}}>♂</span>,
    NONE: null
}

const App = () => {
    const [seed, setSeed] = useLocalStorage('seed', '')
    const [spawns, setSpawns] = useLocalStorage('spawns', '10')
    const [dexComplete, setDexComplete] = useLocalStorage('complete', false)
    const [dexPerfect, setDexPerfect] = useLocalStorage('perfect', false)
    const [shinyCharm, setShinyCharm] = useLocalStorage('shinyCharm', false)
    const [useAggressiveAlgorithm, setUseAggressiveAlgorithm] = useLocalStorage('agro', "1.0.2")
    const [fixedGender, setFixedGender] = useLocalStorage('fixedGender', false)
    const isContinue = !!queryParams['continue']
    const previous = useRef([dexComplete, dexPerfect, shinyCharm])
    const rolls = useRef()
    const lockAvailable = useRef(true)
    const [showSpinner, setShowSpinner] = useState(false)
    const [searchResults, setSearchResults] = useState({})

    const onSubmit = useCallback(async (e) => {
        e.preventDefault()
        if (!lockAvailable.current)
            return
        lockAvailable.current = false
        try {
            const data = {
                seed,
                species: pkmn && pkmn.dex || 10,
                spawns: Number.parseInt(spawns),
                rolls: rolls.current,
                agro: useAggressiveAlgorithm
            }

            setShowSpinner(true)
            const response = await fetch('/holistic-search', {
                method: 'POST',
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(data)
            })
            const result = await response.json()
            setSearchResults(result)
        } catch (e) {
            console.error(e)
            setSearchResults({})
        } finally {
            lockAvailable.current = true
            setShowSpinner(false)
        }
    })

    useEffect(() => {
        const [pComplete, pPerfect, pShinyCharm] = previous.current

        if (!pShinyCharm && shinyCharm) {
            setDexComplete(true)
        }

        if (!pPerfect && dexPerfect) {
            setDexComplete(true)
        }

        if (pComplete && !dexComplete) {
            setDexPerfect(false)
            setShinyCharm(false)
        }

        rolls.current = 26 +
            ((dexComplete) ? 1 : 0) +
            ((dexPerfect) ? 2 : 0) +
            ((shinyCharm) ? 3 : 0)

        previous.current = [dexComplete, dexPerfect, shinyCharm]
    }, [dexComplete, dexPerfect, shinyCharm]);

    return [
        <Header/>,
        <section className="body">
            <form noValidate autoComplete="off" onSubmit={onSubmit}>
                <TextField className="pla-text-input" value={seed} onChange={(e) => setSeed(e.target.value)} name="seed" id="seed" label="Seed" variant="outlined"/>
                <TextField className="pla-text-input" value={spawns} onChange={(e) => setSpawns(e.target.value)} name="spawns" id="spawns" label="Spawns" variant="outlined"
                           type="number"/>
                {/*<FormControl variant="outlined">*/}
                {/*    <InputLabel id="game-version-label">Version</InputLabel>*/}
                {/*    <Select*/}
                {/*        labelId="game-version-label"*/}
                {/*        id="game-version-select"*/}
                {/*        value={gameVersion}*/}
                {/*        onChange={(e) => setGameVersion(e.target.value)}*/}
                {/*        label="Version"*/}
                {/*    >*/}
                {/*        <MenuItem value="">*/}
                {/*            <em>None</em>*/}
                {/*        </MenuItem>*/}
                {/*        <MenuItem value={"1.0.2"}>v1.0.2</MenuItem>*/}
                {/*        <MenuItem value={"1.1.0"}>v1.1.0</MenuItem>*/}
                {/*    </Select>*/}
                {/*</FormControl>*/}
                <Button variant="contained" size="large" type="submit" className="search">
                    Search
                    {showSpinner && <CircularProgress hidden={true} size={20} color="inherit"/>}
                </Button>
                <br/>
                <FormControlLabel control={<Switch checked={useAggressiveAlgorithm} onChange={() => setUseAggressiveAlgorithm(!useAggressiveAlgorithm)}/>}
                                  label={useAggressiveAlgorithm ? "Aggressive" : "Passive"}/>
                <FormControlLabel control={<Switch disabled={!!pkmn} checked={fixedGender} onChange={() => setFixedGender(!fixedGender)}/>}
                                  label={fixedGender ? "Fixed Gender" : "Non Fixed Gender"}/>
                <br/>
                <FormControlLabel
                    control={<Checkbox name="rolls-complete" color="primary" checked={dexComplete} onChange={() => setDexComplete(!dexComplete)}/>}
                    label="Complete"
                />
                <FormControlLabel
                    control={<Checkbox name="rolls-perfect" color="primary" checked={dexPerfect} onChange={() => setDexPerfect(!dexPerfect)}/>}
                    label="Perfect"
                />
                <FormControlLabel
                    control={<Checkbox name="rolls-shiny-charm" color="primary" checked={shinyCharm} onChange={() => setShinyCharm(!shinyCharm)}/>}
                    label="Shiny Charm"
                />
            </form>
            {!!searchResults.results && <SearchResults data={searchResults.results} spawns={spawns}/>}
        </section>,
        <Footer/>
    ]
}
ReactDOM.render(
    <App/>,
    document.getElementById('root')
);

function loadPokemon() {
    const fixedGender = [
        32, 33, 34, 106, 107, 128, 236, 237, 313, 381, 414, 475, 538, 539, 627, 628, 641, 642, 645, 859, 860, 861, 29, 30, 31, 113, 115, 124, 238, 241, 242, 314, 380, 413, 416, 440, 478, 488, 548, 549, 629, 630, 669, 670, 671,
        758, 761, 762, 763, 856, 857, 858, 868, 869, 905, 81, 82, 100, 101, 120, 121, 132, 137, 144, 145, 146, 150, 151, 201, 233, 243, 244, 245, 249, 250, 251, 292, 337, 338, 343, 344, 374, 375, 376, 377, 378, 379, 382, 383,
        384, 385, 386, 436, 437, 462, 474, 479, 480, 481, 482, 483, 484, 486, 487, 489, 490, 491, 492, 493, 494, 599, 600, 601, 615, 622, 623, 638, 639, 640, 643, 644, 646, 647, 648, 649, 703, 716, 717, 718, 719, 720, 721, 772,
        773, 774, 781, 785, 786, 787, 788, 789, 790, 791, 792, 793, 794, 795, 796, 797, 798, 799, 800, 801, 802, 803, 804, 805, 806, 807, 808, 809, 854, 855, 870, 880, 881, 882, 883, 888, 889, 890
    ]
    return (
        (data) => Object.fromEntries(
            data.map(([name, image, num]) => [num || image, {
                name,
                image: 'https://www.serebii.net/legendsarceus/pokemon/small/' + image + '.png',
                dex: parseInt(num || image),
                fixedGender: fixedGender.includes(parseInt(num || image))
            }])
        )
    )([["Rowlet", "722"], ["Dartrix", "723"], ["Decidueye", "724-h", "724"], ["Cyndaquil", "155"], ["Quilava", "156"], ["Typhlosion", "157-h", "157"], ["Oshawott", "501"], ["Dewott", "502"], ["Samurott", "503-h", "503"], ["Bidoof", "399"], ["Bibarel", "400"], ["Starly", "396"], ["Staravia", "397"], ["Staraptor", "398"], ["Shinx", "403"], ["Luxio", "404"], ["Luxray", "405"], ["Wurmple", "265"], ["Silcoon", "266"], ["Beautifly", "267"], ["Cascoon", "268"], ["Dustox", "269"], ["Ponyta", "77"], ["Rapidash", "78"], ["Eevee", "133"], ["Vaporeon", "134"], ["Jolteon", "135"], ["Flareon", "136"], ["Espeon", "196"], ["Umbreon", "197"], ["Leafeon", "470"], ["Glaceon", "471"], ["Sylveon", "700"], ["Zubat", "41"], ["Golbat", "42"], ["Crobat", "169"], ["Drifloon", "425"], ["Drifblim", "426"], ["Kricketot", "401"], ["Kricketune", "402"], ["Buizel", "418"], ["Floatzel", "419"], ["Burmy", "412"], ["Wormadam", "413"], ["Mothim", "414"], ["Geodude", "74"], ["Graveler", "75"], ["Golem", "76"], ["Stantler", "234"], ["Wyrdeer", "899"], ["Munchlax", "446"], ["Snorlax", "143"], ["Paras", "46"], ["Parasect", "47"], ["Pichu", "172"], ["Pikachu", "25"], ["Raichu", "26"], ["Abra", "63"], ["Kadabra", "64"], ["Alakazam", "65"], ["Chimchar", "390"], ["Monferno", "391"], ["Infernape", "392"], ["Buneary", "427"], ["Lopunny", "428"], ["Cherubi", "420"], ["Cherrim", "421"], ["Psyduck", "54"], ["Golduck", "55"], ["Combee", "415"], ["Vespiquen", "416"], ["Scyther", "123"], ["Kleavor", "900"], ["Scizor", "212"], ["Heracross", "214"], ["Mimejr.", "439"], ["Mr.mime", "122"], ["Aipom", "190"], ["Ambipom", "424"], ["Magikarp", "129"], ["Gyarados", "130"], ["Shellos", "422"], ["Gastrodon", "423"], ["Qwilfish", "211-h", "211"], ["Overqwil", "904"], ["Happiny", "440"], ["Chansey", "113"], ["Blissey", "242"], ["Budew", "406"], ["Roselia", "315"], ["Roserade", "407"], ["Carnivine", "455"], ["Petilil", "548"], ["Lilligant", "549-h", "549"], ["Tangela", "114"], ["Tangrowth", "465"], ["Barboach", "339"], ["Whiscash", "340"], ["Croagunk", "453"], ["Toxicroak", "454"], ["Ralts", "280"], ["Kirlia", "281"], ["Gardevoir", "282"], ["Gallade", "475"], ["Yanma", "193"], ["Yanmega", "469"], ["Hippopotas", "449"], ["Hippowdon", "450"], ["Pachirisu", "417"], ["Stunky", "434"], ["Skuntank", "435"], ["Teddiursa", "216"], ["Ursaring", "217"], ["Ursaluna", "901"], ["Goomy", "704"], ["Sliggoo", "705-h", "705"], ["Goodra", "706-h", "706"], ["Onix", "95"], ["Steelix", "208"], ["Rhyhorn", "111"], ["Rhydon", "112"], ["Rhyperior", "464"], ["Bonsly", "438"], ["Sudowoodo", "185"], ["Lickitung", "108"], ["Lickilicky", "463"], ["Togepi", "175"], ["Togetic", "176"], ["Togekiss", "468"], ["Turtwig", "387"], ["Grotle", "388"], ["Torterra", "389"], ["Porygon", "137"], ["Porygon2", "233"], ["Porygon-z", "474"], ["Gastly", "92"], ["Haunter", "93"], ["Gengar", "94"], ["Spiritomb", "442"], ["Murkrow", "198"], ["Honchkrow", "430"], ["Unown", "201"], ["Spheal", "363"], ["Sealeo", "364"], ["Walrein", "365"], ["Remoraid", "223"], ["Octillery", "224"], ["Skorupi", "451"], ["Drapion", "452"], ["Growlithe", "058-h", "58"], ["Arcanine", "059-h", "59"], ["Glameow", "431"], ["Purugly", "432"], ["Machop", "66"], ["Machoke", "67"], ["Machamp", "68"], ["Chatot", "441"], ["Duskull", "355"], ["Dusclops", "356"], ["Dusknoir", "477"], ["Piplup", "393"], ["Prinplup", "394"], ["Empoleon", "395"], ["Mantyke", "458"], ["Mantine", "226"], ["Basculin", "550-w"], ["Basculegion", "902"], ["Vulpix", "37"], ["Ninetales", "38"], ["Tentacool", "72"], ["Tentacruel", "73"], ["Finneon", "456"], ["Lumineon", "457"], ["Magby", "240"], ["Magmar", "126"], ["Magmortar", "467"], ["Magnemite", "81"], ["Magneton", "82"], ["Magnezone", "462"], ["Bronzor", "436"], ["Bronzong", "437"], ["Elekid", "239"], ["Electabuzz", "125"], ["Electivire", "466"], ["Gligar", "207"], ["Gliscor", "472"], ["Gible", "443"], ["Gabite", "444"], ["Garchomp", "445"], ["Nosepass", "299"], ["Probopass", "476"], ["Voltorb", "100-h", "100"], ["Electrode", "101-h", "101"], ["Rotom", "479"], ["Chingling", "433"], ["Chimecho", "358"], ["Misdreavus", "200"], ["Mismagius", "429"], ["Cleffa", "173"], ["Clefairy", "35"], ["Clefable", "36"], ["Sneasel", "215-h", "215"], ["Sneasler", "903"], ["Weavile", "461"], ["Snorunt", "361"], ["Glalie", "362"], ["Froslass", "478"], ["Cranidos", "408"], ["Rampardos", "409"], ["Shieldon", "410"], ["Bastiodon", "411"], ["Swinub", "220"], ["Piloswine", "221"], ["Mamoswine", "473"], ["Bergmite", "712"], ["Avalugg", "713-h", "713"], ["Snover", "459"], ["Abomasnow", "460"], ["Zorua", "570-h", "570"], ["Zoroark", "571-h", "571"], ["Rufflet", "627"], ["Braviary", "628-h", "628"], ["Riolu", "447"], ["Lucario", "448"], ["Uxie", "480"], ["Mesprit", "481"], ["Azelf", "482"], ["Heatran", "485"], ["Regigigas", "486"], ["Cresselia", "488"], ["Tornadus", "641"], ["Thundurus", "642"], ["Landorus", "645"], ["Enamorus", "905"], ["Dialga", "483"], ["Palkia", "484"], ["Giratina", "487"], ["Arceus", "493"], ["Phione", "489"], ["Manaphy", "490"], ["Shaymin", "492"], ["Darkrai", "491"]])
}

