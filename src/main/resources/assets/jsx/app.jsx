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
    TextField,
    Toolbar,
    Typography
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
        queryParams[item.split("=")[0]] = item.split("=")[1]
    })

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

const SearchResult = ({result}) => (
    // null
    <div>{
        result.advances.map((advance, i) => (
            <Accordion key={i}>
                <AccordionSummary
                    expandIcon={<SvgIcon>
                        <path d="m12 8-6 6 1.41 1.41L12 10.83l4.59 4.58L18 14z"/>
                    </SvgIcon>}
                    aria-controls="panel1a-content"
                    id="panel1a-header"
                >
                    <Typography>Advance {i}: {[...toActionDescription(advance.actions), ...((result.advances.length - 1 === i) ? [] : ['Return to town'])].join(", ")}</Typography>
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
                                <ListItem key={reseed.groupSeed}>
                                    {/*{`"seed": "C990BE9317900E1F","sidtid": 3032674752,"pid": 3554362865,"ivs": {"hp": 24,  "att": 20,  "def": 19,  "spAtt": 1,  "spDef": 12,  "speed": 15,  "evs": "110000"},"nature": "Brave","gender": 43,"shiny": false,"alpha": false}`}*/}
                                    <ListItemText key={p.sidtid}
                                                  secondary={
                                                      <div>
                                                          {`Seed: ${p.seed} EVs: ${p.ivs.evs} Nature: ${p.nature} Gender: ${p.gender}`}
                                                          {' Shiny: '}
                                                          <span style={{color: p.shiny ? 'green' : undefined}}>{`${p.shiny}`}</span>
                                                          {' Alpha: '}
                                                          <span style={{color: p.alpha ? 'green' : undefined}}>{`${p.alpha}`}</span>
                                                      </div>
                                                  }
                                    />
                                </ListItem>
                            ))
                        ])}
                    </List>
                </AccordionDetails>
            </Accordion>
        ))
    }</div>
)

const SearchResults = ({data}) => {
    console.log(`<SearchResults data="${data}"/>`)
    if (!data || data.length === 0) {
        return <span>No results found with 10 actions or less. Search stopped.</span>
    }

    if (data.length === 1)
        return <SearchResult result={data[0]}/>

    return data.map((result, i) => (
            <Accordion key={i}>
                <AccordionSummary
                    expandIcon={<SvgIcon>
                        <path d="m12 8-6 6 1.41 1.41L12 10.83l4.59 4.58L18 14z"/>
                    </SvgIcon>}
                    aria-controls="panel1a-content"
                    id="panel1a-header"
                >
                    <Typography>{result.path}</Typography>
                </AccordionSummary>
                <AccordionDetails>
                    <SearchResult result={result}/>
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

const App = () => {
    const [seed, setSeed] = useLocalStorage('seed', '')
    const [spawns, setSpawns] = useLocalStorage('spawns', '10')
    const [dexComplete, setDexComplete] = useLocalStorage('complete', false)
    const [dexPerfect, setDexPerfect] = useLocalStorage('perfect', false)
    const [shinyCharm, setShinyCharm] = useLocalStorage('shinyCharm', false)
    const [gameVersion, setGameVersion] = useLocalStorage('gameVersion', "1.0.2")
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
                spawns: Number.parseInt(spawns),
                rolls: rolls.current,
                genderRatio: [50, 50],
                gameVersion
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
               <FormControl variant="outlined">
                <InputLabel id="game-version-label">Version</InputLabel>
                <Select
                    labelId="game-version-label"
                    id="game-version-select"
                    value={gameVersion}
                    onChange={(e) => setGameVersion(e.target.value)}
                    label="Version"
                >
                    <MenuItem value="">
                        <em>None</em>
                    </MenuItem>
                    <MenuItem value={"1.0.2"}>v1.0.2</MenuItem>
                    <MenuItem value={"1.1.0"}>v1.1.0</MenuItem>
                </Select>
               </FormControl>
                <Button variant="contained" size="large" type="submit">
                    Search
                    {showSpinner && <CircularProgress hidden={true} size={20} color="inherit"/>}
                </Button>
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
            {!!searchResults.results && <SearchResults data={searchResults.results}/>}
        </section>,
        <Footer/>
    ]
}
ReactDOM.render(
    <App/>,
    document.getElementById('root')
);